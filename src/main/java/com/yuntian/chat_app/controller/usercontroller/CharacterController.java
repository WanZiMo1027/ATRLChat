package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/character")
public class CharacterController {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private FollowService followService;



    /**
     * 新增角色
     */
    @RequestMapping("/add")
    public Result addCharacter(@RequestParam Character character){
        characterService.addCharacter(character);
        return Result.success(character.getId());
    }


    /**
     * 我的模型角色列表
     * @return
     */
    @GetMapping("/my-list")
    public Result getCharacterList(){


        List<Character> characters = characterService.getCharacterList();

        // 构建符合要求的响应格式
        Map<String, Object> response = new HashMap<>();
        response.put("characters", characters);

        return Result.success(response);
    }

    /**角色广场
     *
     */
    @GetMapping("/square")
    public Result getCharacterSquare(){
        List<Character> characters = characterService.getPublicCharacter();
        return Result.success(characters);
    }

    /**
     * 获取角色详情
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public Result getCharacterById(@PathVariable Long id) {
        Character character = characterService.getCharacterById(id);
        if (character == null) {
            return Result.error("角色不存在");
        }
        return Result.success(character);
    }

    /**
     * 关注角色
     * @param id 角色ID
     */
    @PostMapping("/follow")
    public Result followCharacter(@RequestParam Long id){
        Boolean followCharacter = followService.followCharacter(id);
        if (followCharacter){
            return Result.success("关注成功");
        }
        return Result.success("取消关注");
    }

}
