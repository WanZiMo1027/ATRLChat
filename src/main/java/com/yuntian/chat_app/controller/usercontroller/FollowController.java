package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 关注角色或取消关注
     * @param id 角色ID
     */
    @PostMapping("/follow")
    public Result<String> followCharacter(@RequestParam Long id){
        Boolean followCharacter = followService.followCharacter(id);
        if (followCharacter){
            return Result.success("关注成功");
        }
        return Result.success("取消关注");
    }

    /**
     * 查看关注状态
     * @param id 角色ID
     * @return 关注状态
     */
    @GetMapping("/status")
    public Result getFollowStatus(@RequestParam Long id){
        Long userId = BaseContext.getCurrentId();
        Boolean followCharacter = followService.isFollowCharacter(id,userId);
        return Result.success(followCharacter);
    }

    /**
     * 查看关注列表
     * @return 关注列表
     */
    @GetMapping("/list")
    public Result<List<CharacterFollowVo>> getFollowList(){
        Long userId = BaseContext.getCurrentId();
        List<CharacterFollowVo> followList = followService.getFollowList(userId);
        return Result.success(followList);
    }

    /**
     * 查看当前角色被关注数量
     * @param id 角色ID
     * @return 关注数量
     */
     @GetMapping("/count")
    public Result<Integer> getFollowCount(@RequestParam Long id) {
         Integer followCount = followService.getFollowCount(id);
         log.info("获取角色被关注数量 - 角色ID: {}, 关注数量: {}", id, followCount);
         return Result.success(followCount);
     }

    /**
     * 关注排行榜
     * @return 关注排行榜
     */
     @GetMapping("/rank")
    public Result<List<CharacterFollowVo>> getFollowRank(@RequestParam(defaultValue = "all") String timeRange,
                                                         @RequestParam(required = false, defaultValue = "10") Integer limit){
         log.info("获取关注排行榜 - 时间范围: {}, 数量: {}", timeRange, limit);
         // 参数校验
         if (!List.of("all", "day", "week", "month").contains(timeRange.toLowerCase())) {
             return Result.error("时间范围参数错误，支持：all/day/week/month");
         }

         if (limit <= 0 || limit > 100) {
             return Result.error("返回数量范围：1-100");
         }
        List<CharacterFollowVo> followRank = followService.getFollowRank(timeRange, limit);
        return Result.success(followRank);
    }
}
