
package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.service.userService.CharacterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        log.info("新增角色，角色名称：{}", character.getName());

        try {
            // 1. 先将角色信息临时存储到Redis
            String tempKey = "temp_character:" + System.currentTimeMillis() + ":" + Thread.currentThread().getId();
            String characterJson = JSONUtil.toJsonStr(character);
            stringRedisTemplate.opsForValue().set(tempKey, characterJson, 300, TimeUnit.SECONDS);
            log.info("角色信息已临时存储到Redis，临时key：{}", tempKey);

            // 2. 将数据插入到MySQL
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

    @Override
    @Transactional
    public void updateCharacterImage(Long characterId, String imageUrl) {
        log.info("更新角色头像，角色ID：{}，头像URL：{}", characterId, imageUrl);

        try {
            // 1. 更新数据库
            int result = characterMapper.updateImage(characterId, imageUrl);
            if (result > 0) {
                log.info("数据库角色头像更新成功，角色ID：{}", characterId);

                // 2. 更新Redis缓存
                updateCharacterImageInRedis(characterId, imageUrl);

            } else {
                log.error("数据库角色头像更新失败，角色ID：{}", characterId);
                throw new RuntimeException("角色头像更新失败");
            }

        } catch (Exception e) {
            log.error("更新角色头像失败：{}", e.getMessage(), e);
            throw new RuntimeException("更新角色头像失败：" + e.getMessage());
        }
    }
    /**
     * 更新Redis中的角色头像
     */
    private void updateCharacterImageInRedis(Long characterId, String imageUrl) {
        try {
            String characterKey = CHARACTER_REDIS_KEY + characterId;
            String characterJson = stringRedisTemplate.opsForValue().get(characterKey);

            if (characterJson != null) {
                Character character = JSONUtil.toBean(characterJson, Character.class);
                character.setImage(imageUrl);

                // 更新Redis缓存
                stringRedisTemplate.opsForValue().set(characterKey,
                        JSONUtil.toJsonStr(character), 7, TimeUnit.DAYS);

                log.info("Redis角色头像更新成功，角色ID：{}", characterId);
            }

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
}
