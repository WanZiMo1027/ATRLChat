package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.ConsultantService;
import com.yuntian.chat_app.service.userService.userServiceImpl.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private ChatHistoryService chatHistoryService;



    @GetMapping(value = "/ai/chat", produces = "text/html;charset=UTF-8")
    public String chatStream(String memoryId,String userId, String characterId, String message, Character character) {
        Long userIdLong = Long.parseLong(userId);

        // 如果没有传入memoryId，使用默认会话
        if (memoryId == null || memoryId.isEmpty()) {
            memoryId = "chat_" + userIdLong + "_" + characterId; // 默认会话
        }


        // 更新会话活动时间
        chatHistoryService.updateSessionActivity(memoryId);

        String name = character != null && character.getName() != null ? character.getName() : "";
        String appearance = character != null && character.getAppearance() != null ? character.getAppearance() : "";
        String background = character != null && character.getBackground() != null ? character.getBackground() : "";
        String personality = character != null && character.getPersonality() != null ? character.getPersonality() : "";
        String classicLines = character != null && character.getClassicLines() != null ? character.getClassicLines() : "";

        return consultantService.chat(memoryId, message, name, appearance, background, personality, classicLines);
    }


    /**
     * 获取单个会话的历史记录
     */
    @GetMapping("/ai/chat/history/{userId}/{characterId}")
    public Result<List<Map<String, String>>> getChatHistory(@PathVariable String userId, @PathVariable String characterId) {
        Long userIdLong = Long.parseLong(userId);
        String memoryId = "chat_" + userIdLong + "_" + characterId; // 默认会话
        chatHistoryService.ensureSessionTracked(userIdLong, characterId, memoryId);
        List<Map<String, String>> history = chatHistoryService.getChatHistory(memoryId);
        return Result.success(history);
    }

    /**
     * 获取指定会话的历史记录
     */
    @GetMapping("/ai/chat/history/session/{sessionId}")
    public Result<List<Map<String, String>>> getSessionHistory(@PathVariable String sessionId) {
        List<Map<String, String>> history = chatHistoryService.getChatHistory(sessionId);
        return Result.success(history);
    }

    /**
     * 获取用户与指定角色的所有会话列表
     */
    @GetMapping("/ai/chat/sessions/{userId}/{characterId}")
    public Result<List<Map<String, Object>>> getHistoryList(@PathVariable String userId, @PathVariable String characterId) {
        Long userIdLong = Long.parseLong(userId);
        List<Map<String, Object>> historyList = chatHistoryService.getHistoryList(userIdLong, characterId);
        return Result.success(historyList);
    }

    /**
     * 创建新的聊天会话
     */
    @PostMapping("/ai/chat/new/{characterId}")
    public Result<String> newChat(@PathVariable String characterId, @RequestBody(required = false) Map<String, String> request) {
        Long userId = BaseContext.getCurrentId(); // 或者从参数获取
        String title = request != null ? request.get("title") : null;
        String sessionId = chatHistoryService.createNewSession(userId, characterId, title);
        return Result.success(sessionId);
    }
}
