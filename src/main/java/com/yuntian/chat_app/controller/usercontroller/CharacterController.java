package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    

}
