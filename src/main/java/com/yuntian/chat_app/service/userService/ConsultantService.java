package com.yuntian.chat_app.service.userService;



import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;


@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",//指定模型
        chatMemoryProvider = "chatMemoryProvider"   // 配置会话记忆功能    配置的是bean的名字    默认是chatMemoryProvider
)
public interface ConsultantService {

    //用于聊天的方法
    @SystemMessage(fromResource = "system.txt")
    public String chat(@MemoryId String memoryId, @UserMessage String message) ;

}
