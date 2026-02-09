package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.ConsultantService;
import com.yuntian.chat_app.service.userService.PrivateChatMessageService;
import com.yuntian.chat_app.service.userService.RagService;
import com.yuntian.chat_app.service.userService.userServiceImpl.ChatHistoryService;
import dev.langchain4j.data.message.ImageContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ChatController {

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private RagService ragService;

    @Autowired
    private PrivateChatMessageService privateChatMessageService;

    /**
     * AI 对话核心接口
     */
    @PostMapping(value = "/ai/chat", produces = "text/html;charset=UTF-8")
    public String chatStream(String memoryId, String userId, String characterId, String message,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             Character character) {
        try {
            Long userIdLong = Long.parseLong(userId);
            Long charIdLong = Long.parseLong(characterId);

            // 1. 如果前端没传 memoryId（新会话），生成一个新的
            // 格式：chat:用户ID_角色ID_时间戳
            if (memoryId == null || memoryId.isEmpty()) {
                memoryId = "chat:" + userIdLong + "_" + characterId + "_" + System.currentTimeMillis();
            }

            // 2. 设置监控上下文
            MonitorContext monitorContext = MonitorContext.builder()
                    .userId(userId).characterId(characterId).memoryId(memoryId).build();
            MonitorContextHolder.setContext(monitorContext);

            // 3. 维护 Redis 会话列表 (让这个会话出现在左侧列表里，且排在最前)
            chatHistoryService.ensureSessionTracked(userIdLong, characterId, memoryId);
            chatHistoryService.updateSessionActivity(memoryId);

            // 4. RAG 检索 (基于 memoryId 检索当前会话的上下文记忆)
            String longTermMemory = ragService.retrieveMemory(memoryId, message);

            // 5. 准备角色属性
            String name = character != null && character.getName() != null ? character.getName() : "";
            String appearance = character != null && character.getAppearance() != null ? character.getAppearance() : "";
            String background = character != null && character.getBackground() != null ? character.getBackground() : "";
            String personality = character != null && character.getPersonality() != null ? character.getPersonality() : "";
            String classicLines = character != null && character.getClassicLines() != null ? character.getClassicLines() : "";

            // 6. 处理图片 & 记录用户消息入库
            ImageContent imageContent = null;
            String dbImageUrl = null; // 用于存入数据库的图片标识或URL

            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
                    String mimeType = imageFile.getContentType();
                    imageContent = ImageContent.from(base64Image, mimeType);
                    dbImageUrl = "[图片已发送]"; // 暂存标记，如果你有OSS，这里应该存OSS URL
                } catch (Exception e) {
                    return String.valueOf(Result.error("图片处理失败: " + e.getMessage()));
                }
            }

            // 保存用户消息到 MySQL
            privateChatMessageService.saveMessage(memoryId, userIdLong, charIdLong, "USER", message, dbImageUrl);

            // 7. 调用 AI (Redis 仅作为短期上下文缓冲区)
            String response;
            if (imageContent != null) {
                response = consultantService.chat(memoryId, message, imageContent, name, appearance,
                        background, personality, classicLines, longTermMemory);
            } else {
                response = consultantService.chat(memoryId, message, name, appearance,
                        background, personality, classicLines, longTermMemory);
            }

            // 8. 处理 AI 响应
            if (response != null && !response.startsWith("Result(")) {
                // 保存 AI 回复到 MySQL
                privateChatMessageService.saveMessage(memoryId, userIdLong, charIdLong, "AI", response, null);

                // 异步存入 RAG 向量库 (用于未来的语义检索)
                ragService.ingestMemory(memoryId, message, response,userIdLong,charIdLong);
            }

            return response;

        } catch (Exception e) {
            log.error("聊天异常", e);
            return String.valueOf(Result.error("聊天失败: " + e.getMessage()));
        } finally {
            MonitorContextHolder.clearContext();
        }
    }


    /**
     * 获取指定会话的历史记录
     */
    @GetMapping("/ai/chat/history/session/{sessionId}")
    public Result<List<Map<String, String>>> getSessionHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        // 🔥【持久化-读】改用 privateChatMessageService 查数据库
        List<Map<String, String>> history = privateChatMessageService.getHistory(sessionId, page, size);
        return Result.success(history);
    }

    /**
     * 获取用户与指定角色的所有会话列表 (查 Redis，保持原样)
     * 这个接口返回的是左侧的“会话列表”，不是具体消息
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
        Long userId = BaseContext.getCurrentId();
        // 创建新会话ID并记录到 Redis 列表
        String sessionId = chatHistoryService.createNewSession(userId, characterId);
        return Result.success(sessionId);
    }

    // 废弃或兼容旧接口：获取单个会话的历史记录
    // 建议前端统一迁移到 getSessionHistory 接口
    @GetMapping("/ai/chat/history/{userId}/{characterId}")
    public Result<List<Map<String, String>>> getChatHistory(@PathVariable String userId, @PathVariable String characterId) {
        // 简单兼容：返回空，或者让前端调新的接口
        return Result.success(List.of());
    }
}