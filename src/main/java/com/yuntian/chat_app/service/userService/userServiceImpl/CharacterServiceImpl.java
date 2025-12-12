package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CharacterServiceImpl implements CharacterService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CharacterMapper characterMapper;

    // Redis key 规范化
    // 单个角色详情
    private static final String CHARACTER_DETAIL_KEY = "character:detail:";
    // 用户角色列表（JSON 数组）
    private static final String CHARACTER_LIST_KEY = "character:list:user:";

    // 临时缓存（可选）
    private static final String TEMP_CHARACTER_KEY_PREFIX = "temp_character:";
    // 角色广场列表
    private static final String CHARACTER_SQUARE_KEY = "character:list:public";

    private static final long CACHE_TTL_DAYS = 7;

    @Override
    @Transactional
    public void addCharacter(Character character) {
        // 1) 从ThreadLocal拿用户ID
        Long currentUserId = BaseContext.getCurrentId();
        log.info("当前用户ID：{}", currentUserId);
        if (currentUserId == null) {
            log.error("无法获取当前用户ID，ThreadLocal中用户ID为空");
            throw new RuntimeException("用户未登录或会话已过期");
        }
        character.setUserId(currentUserId);
        log.info("设置用户ID到角色对象，用户ID：{}", currentUserId);
        log.info("新增角色，角色名称：{}", character.getName());

        // 2) 临时写入 Redis（可选）
        String tempKey = TEMP_CHARACTER_KEY_PREFIX + System.currentTimeMillis() + ":" + Thread.currentThread().getId();
        try {
            String characterJson = JSONUtil.toJsonStr(character);
            stringRedisTemplate.opsForValue().set(tempKey, characterJson, 300, TimeUnit.SECONDS);
            log.info("角色信息已临时存储到Redis，临时key：{}", tempKey);

            // 3) 写入 MySQL
            character.setIsPublic(0);
            int result = characterMapper.insert(character);
            if (result <= 0) {
                throw new RuntimeException("新增角色到MySQL失败");
            }
            log.info("新增角色到MySQL成功，角色ID：{}", character.getId());

            // 4) 更新角色详情缓存，并删除用户列表缓存让其懒加载重建
            updateCharacterDetailCache(character);
            evictUserCharacterListCache(character.getUserId());

            // 5) 删除临时 key
            stringRedisTemplate.delete(tempKey);
            log.info("删除临时key成功，临时key：{}", tempKey);

        } catch (Exception e) {
            // MySQL插入失败或其它异常时，清理临时key
            stringRedisTemplate.delete(tempKey);
            log.error("新增角色失败：{}", e.getMessage(), e);
            throw new RuntimeException("新增角色失败：" + e.getMessage());
        }
    }

    @Override
    public void updateCharacterAvatar(Long characterId, String imageUrl) {
        // 1) 更新数据库
        Character patch = new Character();
        patch.setId(characterId);
        patch.setImage(imageUrl);
        characterMapper.updateById(patch);

        log.info("角色头像URL已更新，角色ID：{}，URL：{}", characterId, imageUrl);

        // 2) 同步更新Redis角色详情缓存
        updateCharacterImageInRedis(characterId, imageUrl);
        log.info("Redis缓存头像已更新（updateCharacterAvatar），角色ID：{}", characterId);

        // 3) 头像更新通常不影响列表数据结构（除非你的列表里也包含 image 字段并需展示）
        // 如果用户列表缓存中需要展示头像，采用“删列表缓存，下次读取重建”
        Character character = characterMapper.selectById(characterId);
        if (character != null && character.getUserId() != null) {
            evictUserCharacterListCache(character.getUserId());
        }
    }

    /**
     * 获取当前用户角色列表
     */
    @Override
    public List<Character> getCharacterList() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            log.error("无法获取当前用户ID");
            throw new RuntimeException("用户未登录");
        }

        String key = CHARACTER_LIST_KEY + currentUserId;
        String characterListJson = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(characterListJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(characterListJson);
                return JSONUtil.toList(array, Character.class);
            } catch (JSONException ex) {
                log.warn("用户角色列表缓存格式异常，将回源DB并重建缓存，key：{}", key, ex);
            }
        }

        log.info("缓存未命中或格式异常，回源DB获取用户角色列表，用户ID：{}", currentUserId);
        List<Character> characters = characterMapper.selectByUserId(currentUserId);

        // 回填缓存（数组 JSON）
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(characters), CACHE_TTL_DAYS, TimeUnit.DAYS);

        return characters;
    }

    /**
     * 更新Redis中的角色头像（仅角色详情缓存）
     */
    private void updateCharacterImageInRedis(Long characterId, String imageUrl) {
        try {
            String characterKey = CHARACTER_DETAIL_KEY + characterId;
            String characterJson = stringRedisTemplate.opsForValue().get(characterKey);

            Character character;
            if (StrUtil.isNotBlank(characterJson)) {
                character = JSONUtil.toBean(characterJson, Character.class);
                character.setImage(imageUrl);
            } else {
                // 从数据库加载并更新
                character = characterMapper.selectById(characterId);
                if (character == null) {
                    log.warn("在数据库中未找到角色，无法更新Redis缓存，角色ID：{}", characterId);
                    return;
                }
                character.setImage(imageUrl);
            }

            stringRedisTemplate
                    .opsForValue()
                    .set(characterKey, JSONUtil.toJsonStr(character), CACHE_TTL_DAYS, TimeUnit.DAYS);

            log.info("Redis角色头像更新成功，角色ID：{}", characterId);

        } catch (Exception e) {
            log.error("更新Redis角色头像失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public Character getCharacterById(Long id) {
        log.info("获取角色详情，角色ID：{}", id);

        String characterKey = CHARACTER_DETAIL_KEY + id;
        String characterJson = stringRedisTemplate.opsForValue().get(characterKey);

        if (StrUtil.isNotBlank(characterJson)) {
            log.info("从Redis缓存获取角色详情，角色ID：{}", id);
            return JSONUtil.toBean(characterJson, Character.class);
        }

        Character character = characterMapper.selectById(id);
        if (character != null) {
            stringRedisTemplate
                    .opsForValue()
                    .set(characterKey, JSONUtil.toJsonStr(character), CACHE_TTL_DAYS, TimeUnit.DAYS);
            log.info("从数据库获取角色详情并缓存到Redis，角色ID：{}", id);
        }

        return character;
    }
    /**
     * 获取所有角色列表（公开模型）
     * @return 角色列表
     */
    @Override
    public List<Character> getPublicCharacter() {
        String characterSquareKey = CHARACTER_SQUARE_KEY;
        String characterSquareJson = stringRedisTemplate.opsForValue().get(characterSquareKey);

        if (StrUtil.isNotBlank(characterSquareJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(characterSquareJson);
                return JSONUtil.toList(array, Character.class);
            } catch (JSONException ex) {
                log.warn("角色广场缓存格式异常，将回源DB并重建缓存，key：{}", characterSquareKey, ex);
            }
        }

        log.info("缓存未命中或格式异常，回源DB获取角色广场列表");
        List<Character> characters = characterMapper.selectAll();

        // 回填缓存（数组 JSON）
        stringRedisTemplate.opsForValue().set(characterSquareKey, JSONUtil.toJsonStr(characters), CACHE_TTL_DAYS, TimeUnit.DAYS);

        return characters;
    }

     /**
     * 检索角色
     * @param name 角色名称
     * @param personality 角色性格
     * @return 角色列表
     */
     @Override
    public List<Character> searchCharacter(String name, String personality) {
        log.info("检索角色，名称：{}，性格：{}", name, personality);
        // 从数据库查询
        List<Character> characters = characterMapper.selectByKeyword(name, personality);
        return characters;
    }

    @Override
    public Integer publicOrNotCharacter(Long characterId) {
        Character character = characterMapper.selectById(characterId);

        if (character == null) {
            log.info("角色不存在，角色ID：{}", characterId);
            throw new RuntimeException("角色不存在");
        }

        // 切换公开状态
        Integer newStatus = character.getIsPublic() == 0 ? 1 : 0;


        character.setIsPublic(newStatus);
        characterMapper.updateIsPublic(characterId, newStatus);

        updateCharacterDetailCache(character);           // 更新详情缓存
        evictUserCharacterListCache(character.getUserId());     // 删除用户列表缓存
        stringRedisTemplate.delete(CHARACTER_SQUARE_KEY);            // 删除广场缓存
        return newStatus;
    }
    /**
     * 删除用户角色列表缓存，读时重建
     */
    private void evictUserCharacterListCache(Long userId) {
        try {
            String userCharacterListKey = CHARACTER_LIST_KEY + userId;
            Boolean deleted = stringRedisTemplate.delete(userCharacterListKey);
            log.info("用户角色列表缓存删除：key={}，deleted={}", userCharacterListKey, deleted);
        } catch (Exception e) {
            log.warn("删除用户角色列表缓存失败，用户ID：{}", userId, e);
        }
    }
    /**
     * 写入/刷新角色详情缓存
     */
    private void updateCharacterDetailCache(Character character) {
        try {
            String characterKey = CHARACTER_DETAIL_KEY + character.getId();
            String characterJson = JSONUtil.toJsonStr(character);
            stringRedisTemplate
                    .opsForValue()
                    .set(characterKey, characterJson, CACHE_TTL_DAYS, TimeUnit.DAYS);
            log.info("角色详情已更新到Redis缓存，角色ID：{}，用户ID：{}", character.getId(), character.getUserId());
        } catch (Exception e) {
            log.error("更新角色详情到Redis失败：{}", e.getMessage(), e);
        }
    }

}