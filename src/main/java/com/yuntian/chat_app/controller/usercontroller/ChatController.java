package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.service.userService.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;



    @GetMapping(value = "/ai/chat",produces = "text/html;charset=UTF-8")
    public String chatStream(String memoryId,String message) {
        return consultantService.chat(memoryId,message);
    }
}
