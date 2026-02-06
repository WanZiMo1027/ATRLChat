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

        //用于带图片的多模态聊天
        @SystemMessage(fromResource = "system.txt")
        public String chat(@MemoryId String memoryId,
                           @UserMessage String message,
                           @UserMessage  ImageContent imageContent,
                           @V("name") String name,
                           @V("appearance") String appearance,
                           @V("background") String background,
                           @V("personality") String personality,
                           @V("classic_lines") String classicLines
        ) ;
        //用于普通聊天的方法
        @SystemMessage(fromResource = "system.txt")
        public String chat(@MemoryId String memoryId,
                         @UserMessage String message,
                         @V("name") String name,
                         @V("appearance") String appearance,
                         @V("background") String background,
                         @V("personality") String personality,
                         @V("classic_lines") String classicLines
        );

    }
