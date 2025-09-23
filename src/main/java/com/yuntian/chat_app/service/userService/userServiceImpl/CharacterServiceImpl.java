
package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
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

    // Redis key前缀
    private static final String CHARACTER_REDIS_KEY = "character:";
    private static final String CHARACTER_LIST_KEY = "character_list:user:";

    @Override
    @Transactional
    public void addCharacter(Character character) {
        // 从ThreadLocal中获取当前用户ID并设置到角色对象中
        Long currentUserId = BaseContext.getCurrentId();
        log.info("当前用户ID：{}",currentUserId);
        if (currentUserId != null) {
            character.setUserId(currentUserId);
            log.info("设置用户ID到角色对象，用户ID：{}", currentUserId);
        } else {
            log.error("无法获取当前用户ID，ThreadLocal中用户ID为空");
            throw new RuntimeException("用户未登录或会话已过期");
        }
        log.info("新增角色，角色名称：{}", character.getName());

        try {
            // 1. 先将角色信息临时存储到Redis
            String tempKey = "temp_character:" + System.currentTimeMillis() + ":" + Thread.currentThread().getId();
            String characterJson = JSONUtil.toJsonStr(character);
            stringRedisTemplate.opsForValue().set(tempKey, characterJson, 300, TimeUnit.SECONDS);
            log.info("角色信息已临时存储到Redis，临时key：{}", tempKey);

            // 2. 将数据插入到MySQL
            character.setIsPublic(0);
            int result = characterMapper.insert(character);
            if (result > 0) {
                log.info("新增角色到MySQL成功，角色ID：{}", character.getId());

                // 3. MySQL插入成功后，将角色更新到Redis正式缓存
                updateCharacterToRedis(character);

                // 4. 删除临时key
                stringRedisTemplate.delete(tempKey);
                log.info("删除临时key成功，临时key：{}", tempKey);

            } else {
                // MySQL插入失败，清理临时Redis数据
                stringRedisTemplate.delete(tempKey);
                log.error("新增角色到MySQL失败，已清理临时key：{}", tempKey);
                throw new RuntimeException("新增角色到MySQL失败");
            }

        } catch (Exception e) {
            log.error("新增角色失败：{}", e.getMessage(), e);
            throw new RuntimeException("新增角色失败：" + e.getMessage());
        }
    }


/*    @Override
    @Transactional
    public void updateCharacterImage(Long characterId, String imageUrl) {
        log.info("更新角色头像，角色ID：{}，头像URL：{}", characterId, imageUrl);

        try {
            // 1. 更新数据库
            int result = characterMapper.updateImage(characterId, imageUrl);
            if (result > 0) {
                log.info("数据库角色头像更新成功，角色ID：{}", characterId);

                // 2. 同步更新Redis缓存中的头像URL（若不存在则创建）
                updateCharacterImageInRedis(characterId, imageUrl);
                log.info("Redis缓存头像已更新，角色ID：{}", characterId);

            } else {
                log.error("数据库角色头像更新失败，角色ID：{}", characterId);
                throw new RuntimeException("角色头像更新失败");
            }

        } catch (Exception e) {
            log.error("更新角色头像失败：{}", e.getMessage(), e);
            throw new RuntimeException("更新角色头像失败：" + e.getMessage());
        }
    }*/

    @Override
    public void updateCharacterAvatar(Long characterId, String imageUrl) {
        Character character = new Character();
        character.setId(characterId);
        character.setImage(imageUrl);
        characterMapper.updateById(character);
        log.info("角色头像URL已更新，角色ID：{}，URL：{}", characterId, imageUrl);
        
        // 同步更新Redis缓存，防止读取到旧值
        updateCharacterImageInRedis(characterId, imageUrl);
        log.info("Redis缓存头像已更新（uploadCharacterAvatar），角色ID：{}", characterId);
    }

    @Override
    public List<Character> getCharacterList() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            log.error("无法获取当前用户ID");
            throw new RuntimeException("用户未登录");
        }

        log.info("获取用户角色列表，用户ID：{}", currentUserId);
        return characterMapper.selectByUserId(currentUserId);
    }
    /**
     * 更新Redis中的角色头像
     */
    private void updateCharacterImageInRedis(Long characterId, String imageUrl) {
        try {
            String characterKey = CHARACTER_REDIS_KEY + characterId;
            String characterJson = stringRedisTemplate.opsForValue().get(characterKey);

            Character character;
            if (characterJson != null) {
                // 更新已缓存的角色信息
                character = JSONUtil.toBean(characterJson, Character.class);
                character.setImage(imageUrl);
            } else {
                // 如果没有缓存，从数据库获取并更新缓存
                character = characterMapper.selectById(characterId);
                if (character == null) {
                    log.warn("在数据库中未找到角色，无法更新Redis缓存，角色ID：{}", characterId);
                    return;
                }
                character.setImage(imageUrl);
            }

            // 更新Redis缓存
            stringRedisTemplate.opsForValue().set(characterKey,
                    JSONUtil.toJsonStr(character), 7, TimeUnit.DAYS);

            log.info("Redis角色头像更新成功，角色ID：{}", characterId);

        } catch (Exception e) {
            log.error("更新Redis角色头像失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 将角色更新到Redis正式缓存
     */
    private void updateCharacterToRedis(Character character) {
        try {
            // 使用String结构存储JSON
            String characterKey = CHARACTER_REDIS_KEY + character.getId();
            String characterJson = JSONUtil.toJsonStr(character);
            stringRedisTemplate.opsForValue().set(characterKey, characterJson, 7, TimeUnit.DAYS);

            // 将角色ID添加到用户的角色列表中
            String userCharacterListKey = CHARACTER_LIST_KEY + character.getUserId();
            stringRedisTemplate.opsForList().leftPush(userCharacterListKey, character.getId().toString());
            stringRedisTemplate.expire(userCharacterListKey, 7, TimeUnit.DAYS);

            log.info("角色信息已更新到Redis缓存，角色ID：{}，用户ID：{}", character.getId(), character.getUserId());

        } catch (Exception e) {
            log.error("更新角色到Redis失败：{}", e.getMessage(), e);
        }
    }

    @Override
    public Character getCharacterById(Long id) {
        log.info("获取角色详情，角色ID：{}", id);

        // 先尝试从Redis缓存获取
        String characterKey = CHARACTER_REDIS_KEY + id;
        String characterJson = stringRedisTemplate.opsForValue().get(characterKey);

        if (characterJson != null) {
            log.info("从Redis缓存获取角色详情，角色ID：{}", id);
            return JSONUtil.toBean(characterJson, Character.class);
        }

        // 从数据库查询
        Character character = characterMapper.selectById(id);
        if (character != null) {
            // 将查询结果缓存到Redis
            stringRedisTemplate.opsForValue().set(characterKey,
                    JSONUtil.toJsonStr(character), 7, TimeUnit.DAYS);
            log.info("从数据库获取角色详情并缓存到Redis，角色ID：{}", id);
        }

        return character;
    }

    @Override
    public List<Character> getPublicCharacter() {
        return characterMapper.selectAll();
    }

    /**
     * 获取所有角色列表（公开模型）
     * @return 角色列表
     */



}
