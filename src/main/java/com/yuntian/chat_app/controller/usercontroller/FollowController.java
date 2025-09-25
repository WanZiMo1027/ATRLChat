package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.UserFollowCharacter;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<List<UserFollowCharacter>> getFollowList(){
        Long userId = BaseContext.getCurrentId();
        List<UserFollowCharacter> followList = followService.getFollowList(userId);
        return Result.success(followList);
    }
}
