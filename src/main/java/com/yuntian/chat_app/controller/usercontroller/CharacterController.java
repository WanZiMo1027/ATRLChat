package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/character")
public class CharacterController {

    @Autowired
    private CharacterService characterService;

    /**
     * 新增角色
     */
    @RequestMapping("/add")
    public Result addCharacter(@RequestBody Character character){
        characterService.addCharacter(character);
        return Result.success(character.getId());
    }


    @GetMapping("/List")
    public Result getCharacterList(){
        List<Character> characters = characterService.getCharacterList();

        // 构建符合要求的响应格式
        Map<String, Object> response = new HashMap<>();
        response.put("characters", characters);

        return Result.success(response);
    }

    

}
