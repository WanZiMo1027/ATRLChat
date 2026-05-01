package com.yuntian.chat_app.service.userService;


import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;


    @AiService(
            wiringMode = AiServiceWiringMode.EXPLICIT,
            chatModel = "customOpenAiChatModel",//指定模型
            chatMemoryProvider = "chatMemoryProvider"   // 配置会话记忆功能    配置的是bean的名字    默认是chatMemoryProvider
            ,tools = "timeTools"
    )
    public interface ConsultantService {

        String RAG_USER_PROMPT = """
            【相关历史记忆（仅供参考，若无关请忽略）】
            {{memory}}
            
            【用户提问】
            {{message}}
            """;

        String GROUP_RAG_USER_PROMPT = """
            【相关历史记忆（仅供参考，若无关请忽略）】
            {{memory}}
            
            【群聊上下文】
            当前发言人：{{sender_name}}
            群聊用户消息格式为“用户名: 消息内容”。请根据用户名区分不同成员的发言，不要把用户名当成正文内容。
            
            【本轮群聊消息】
            {{message}}
            """;

        //用于带图片的多模态聊天
        @SystemMessage(fromResource = "system.txt")
        @UserMessage(RAG_USER_PROMPT)
        public String chat(@MemoryId String memoryId,
                           @V("message") String message,
                           @UserMessage ImageContent imageContent,
                           @V("name") String name,
                           @V("appearance") String appearance,
                           @V("background") String background,
                           @V("personality") String personality,
                           @V("classic_lines") String classicLines,
                           @V("memory") String memory
        ) ;
        //用于普通聊天的方法
        @SystemMessage(fromResource = "system.txt")
        @UserMessage(RAG_USER_PROMPT)
        public String chat(@MemoryId String memoryId,
                         @V("message") String message,
                         @V("name") String name,
                         @V("appearance") String appearance,
                         @V("background") String background,
                         @V("personality") String personality,
                         @V("classic_lines") String classicLines,
                         @V("memory") String memory
        );

        //用于群聊中被 @AI 触发的普通文本聊天
        @SystemMessage(fromResource = "group-system.txt")
        @UserMessage(GROUP_RAG_USER_PROMPT)
        public String groupChat(@MemoryId String memoryId,
                         @V("message") String message,
                         @V("sender_name") String senderName,
                         @V("name") String name,
                         @V("appearance") String appearance,
                         @V("background") String background,
                         @V("personality") String personality,
                         @V("classic_lines") String classicLines,
                         @V("memory") String memory
        );

    }
