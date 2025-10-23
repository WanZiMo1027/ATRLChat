package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.mapper.userMapper.UserFollowCharacterMapper;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.utils.SnowflakeIdGenerator;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.TimeUnit;



@Service
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowCharacterMapper userFollowCharacterMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String FOLLOW_KEY = "follow:";
    private static final String FOLLOW_LIST_KEY = "follow:list:";
    private static final int CACHE_TTL_DAYS = 1;
    private static final String FOLLOW_COUNT_KEY = "follow:count:";
    private static final String RANK_CACHE_KEY = "follow:rank:";
    /**
     * 关注角色或取消关注角色
     * @param id 角色ID
     * @return 是否关注成功
     */
    @Override
    public Boolean followCharacter(Long id) {
        //先检查用户是否关注该角色
        Long userId = BaseContext.getCurrentId();
        String followKey = FOLLOW_KEY + userId + ":" + id;
        String followJson = stringRedisTemplate.opsForValue().get(followKey);

        Boolean result;
        //如果在缓存中查到关系，直接更新状态
        if(StrUtil.isNotBlank(followJson)){
            //将followJson转换为UserFollowCharacter对象
            UserFollowCharacter userFollowCharacter = JSONUtil.toBean(followJson, UserFollowCharacter.class);
            //更新状态
            stringRedisTemplate.delete(FOLLOW_LIST_KEY+userId);
            result = updateStatus(userFollowCharacter, followKey);
        }else {
            //如果未查到则从数据库查询
            UserFollowCharacter rel = userFollowCharacterMapper.selectByUserIdAndCharacterId(userId, id);
            //如果查不到则创建新关系
            if(rel == null){
                log.info("用户未关注该角色，角色ID：{}，用户ID：{}", id, userId);
                SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);
                Long followId = idGenerator.nextId();
                rel = new UserFollowCharacter();
                rel.setFollowId(followId);
                rel.setUserId(userId);
                rel.setCharacterId(id);
                rel.setCreateTime(LocalDateTime.now());
                rel.setUpdateTime(LocalDateTime.now());
                rel.setStatus(1);
                userFollowCharacterMapper.followCharacter(rel);
                String newFollowJson = JSONUtil.toJsonStr(rel);
                stringRedisTemplate.opsForValue().set(followKey, newFollowJson, CACHE_TTL_DAYS, TimeUnit.DAYS);
                stringRedisTemplate.delete(FOLLOW_LIST_KEY+userId);
                result = rel.getStatus() == 1;
            } else {
                //更新状态
                stringRedisTemplate.delete(FOLLOW_LIST_KEY+userId);
                result = updateStatus(rel, followKey);
            }
        }

        // ⭐ 更新关注数缓存
        updateFollowCountCache(id, result);

        return result;
    }


    /**
     * 查看关注状态
     * @param id 角色ID
     * @param userId 用户ID
     * @return 关注状态
     */
    @Override
    public Boolean isFollowCharacter(Long id, Long userId) {
        UserFollowCharacter userFollowCharacter = userFollowCharacterMapper.selectByUserIdAndCharacterId(userId, id);
        return userFollowCharacter != null && userFollowCharacter.getStatus() == 1;
    }

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    @Override
    public List<CharacterFollowVo> getFollowList(Long userId) {
        //查看缓存中的列表
        String followListJson = stringRedisTemplate.opsForValue().get(FOLLOW_LIST_KEY + userId);
        if(StrUtil.isNotBlank(followListJson)){
            return JSONUtil.toList(followListJson, CharacterFollowVo.class);
        }
        //如果没有则从数据库查询
        List<CharacterFollowVo> followList = userFollowCharacterMapper.selectFollowList(userId);
        //将查询结果转换为JSON字符串并缓存
        String followListJsonCache = JSONUtil.toJsonStr(followList);
        stringRedisTemplate.opsForValue().set(FOLLOW_LIST_KEY + userId, followListJsonCache, CACHE_TTL_DAYS, TimeUnit.DAYS);
        return followList;
    }

    /**
     * 获取角色被关注数量
     * @param id 角色ID
     * @return 关注数量
     */
    @Override
    public Integer getFollowCount(Long id) {
        String followCountJson = stringRedisTemplate.opsForValue().get(FOLLOW_COUNT_KEY + id);
        if(StrUtil.isNotBlank(followCountJson)){
            return Integer.parseInt(followCountJson);
        }
        //从数据库查询关注数量
        Integer followCount = userFollowCharacterMapper.selectFollowCount(id);
        stringRedisTemplate.opsForValue().set(FOLLOW_COUNT_KEY + id, String.valueOf(followCount), CACHE_TTL_DAYS, TimeUnit.DAYS);
        log.info("获取角色被关注数量 - 角色ID: {}, 关注数量: {}", id, followCount);
        return followCount;
    }

    /**
     * 关注排行榜
     * @param timeRange 时间范围all/day/week/month
     * @param limit 返回数量
     * @return 关注排行榜
     */
    @Override
    public List<CharacterFollowVo> getFollowRank(String timeRange, Integer limit) {
        if(limit == null || limit <= 0){
            limit = 10;
        }
        if(StrUtil.isBlank(timeRange)){
            timeRange = "all";
        }
        String key = RANK_CACHE_KEY + timeRange + ":" + limit;
        //从缓存中获取关注排行榜
        List<CharacterFollowVo> followRank = getCacheRank(key);
        if(!followRank.isEmpty()){
            //缓存中存在数据，直接返回
            return followRank;
        }
        LocalDateTime startTime = startTime(timeRange);
        LocalDateTime endTime = LocalDateTime.now();
        log.info("获取关注排行榜 - 时间范围: {}, 开始时间: {}, 结束时间: {}, 返回数量: {}", timeRange, startTime, endTime, limit);
        //从数据库查询关注排行榜
        followRank = userFollowCharacterMapper.selectFollowRank(startTime, endTime, limit);
        //设置排名
        for(int i = 0; i < followRank.size(); i++){
            followRank.get(i).setRank(i+1);
        }
        //将查询结果转换为JSON字符串并缓存
        String followRankJsonCache = JSONUtil.toJsonStr(followRank);
        long expireTime;
        TimeUnit timeUnit;
        switch (timeRange.toLowerCase()){
            case "day":
                expireTime = 10;
                timeUnit = TimeUnit.MINUTES;
                break;
            case "week":
                expireTime = 20;
                timeUnit = TimeUnit.MINUTES;
                break;
            case "month":
                expireTime = 1;
                timeUnit = TimeUnit.HOURS;
                break;
            case "all":
            default:
                expireTime = 2;
                timeUnit = TimeUnit.HOURS;
                break;
        }
        stringRedisTemplate.opsForValue().set(key, followRankJsonCache, expireTime, timeUnit);
        return followRank;
    }

    /**
     * 从缓存中获取关注排行榜
     * @param key 缓存键
     * @return 关注排行榜
     */
    private List<CharacterFollowVo> getCacheRank(String key){
        String followRankJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(followRankJson)){
            return JSONUtil.toList(followRankJson, CharacterFollowVo.class);
        }
        return List.of();
    }

        /**
         * 根据时间范围获取开始时间
         * @param timeRange 时间范围
         * @return 开始时间
         */
        private LocalDateTime startTime(String timeRange){
            LocalDate now = LocalDate.now();
            switch (timeRange.toLowerCase()){
                case "day":
                    return now.atStartOfDay();
                case "week":
                    return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            .atStartOfDay();
                case "month":
                    return now.withDayOfMonth(1).atStartOfDay();
                case "all":
                default:
                    //不限时间，返回null
                    return null;
            }
        }



    //更新状态
    private Boolean updateStatus(UserFollowCharacter userFollowCharacter, String followKey){
        int current = userFollowCharacter.getStatus() == null ? 0 : userFollowCharacter.getStatus();
        int next = current == 1 ? 0 : 1;
        userFollowCharacter.setStatus(next);
        userFollowCharacter.setUpdateTime(LocalDateTime.now());
        userFollowCharacterMapper.updateById(userFollowCharacter);
        //更新缓存
        String newFollowJson = JSONUtil.toJsonStr(userFollowCharacter);
        stringRedisTemplate.opsForValue().set(followKey, newFollowJson, CACHE_TTL_DAYS, TimeUnit.DAYS);
        return next == 1;
    }

    /**
     * 更新角色关注数缓存
     * @param characterId 角色ID
     * @param isFollow true表示关注，false表示取消关注
     */
    private void updateFollowCountCache(Long characterId, Boolean isFollow) {
        String countKey = FOLLOW_COUNT_KEY + characterId;
        String countStr = stringRedisTemplate.opsForValue().get(countKey);

        if (StrUtil.isNotBlank(countStr)) {
            try {
                // 缓存存在，进行增量更新
                int currentCount = Integer.parseInt(countStr);
                int newCount = isFollow ? currentCount + 1 : Math.max(0, currentCount - 1);
                stringRedisTemplate.opsForValue().set(countKey, String.valueOf(newCount), CACHE_TTL_DAYS, TimeUnit.DAYS);
                log.info("更新关注数缓存 - 角色ID: {}, 操作: {}, 新计数: {}", characterId, isFollow ? "关注" : "取消", newCount);
            } catch (NumberFormatException e) {
                // 缓存值异常，删除让其重新加载
                log.warn("关注数缓存格式错误，删除缓存 - 角色ID: {}", characterId);
                stringRedisTemplate.delete(countKey);
            }
        } else {
            // 缓存不存在，从数据库查询并设置
            Integer followCount = userFollowCharacterMapper.selectFollowCount(characterId);
            if (followCount != null) {
                stringRedisTemplate.opsForValue().set(countKey, String.valueOf(followCount), CACHE_TTL_DAYS, TimeUnit.DAYS);
                log.info("初始化关注数缓存 - 角色ID: {}, 计数: {}", characterId, followCount);
            }
        }
    }
}
