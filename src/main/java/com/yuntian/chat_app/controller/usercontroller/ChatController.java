package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.service.userService.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;



    @GetMapping(value = "/ai/chat",produces = "text/html;charset=UTF-8")
    public String chatStream(String memoryId, String message, Character character) {
        String name = character != null && character.getName() != null ? character.getName() : "";
        String appearance = character != null && character.getAppearance() != null ? character.getAppearance() : "";
        String background = character != null && character.getBackground() != null ? character.getBackground() : "";
        String personality = character != null && character.getPersonality() != null ? character.getPersonality() : "";
        String classicLines = character != null && character.getClassicLines() != null ? character.getClassicLines() : "";
        return consultantService.chat(memoryId, message, name, appearance, background, personality, classicLines);
    }
}
