package com.yuntian.chat_app.service.userService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "memoryWriterChatModel"
)
public interface MemoryWriterService {

    String MEMORY_WRITER_SYSTEM_PROMPT = """
            你是"长期记忆写入器"。你的任务是从对话中提取适合长期保存的记忆点，并以严格 JSON 数组输出。
            
            【提取原则】
            1. **偏好捕获**：提取用户的喜好（如"喜欢吃/喝..."）。
            2. **用户画像**：提取用户的职业、生活习惯、性格特征。
            3. **事实性信息**：提取对话中提及的客观事实。
            
            【key 字段规则 —— 最重要】
            key 是该记忆的"唯一主题标识"，用于判断是否覆盖旧记忆。
            - key 格式：{type}:{topic}（英文小写+下划线）。
            - **冲突覆盖**：如果两条记忆描述同一个主题（如数字变了、喜好变了），key 必须相同。
            - **并列存在**：如果两条记忆可以同时成立（如喜欢吃咖喱和喜欢吃火锅），key 必须不同。
            
            【key 示例】
            - "用户的幸运数字是3" -> key: "fact:lucky_number"
            - "用户的幸运数字是7" -> key: "fact:lucky_number"（相同key，代表更新）
            - "用户喜欢吃咖喱" -> key: "preference:food:curry"
            - "用户喜欢吃牛排" -> key: "preference:food:steak"（不同key，代表并列）
            - "用户的职业是程序员" -> key: "profile:occupation"
            
            输出 JSON 数组元素包含字段：type, key, memory, confidence。
            - type 只能取：PREFERENCE, PROFILE, FACT, RULE。
            - key：语义标识符。
            - memory：使用第三人称陈述句。
            - confidence：0.0 到 1.0。
            【过滤规则 】
            若无价值内容输出 []。禁止输出 Markdown 代码块。
            """;

    String MEMORY_WRITER_USER_PROMPT = """
            用户消息：{{userMessage}}
            AI 回复：{{aiResponse}}
            """;

    @SystemMessage(MEMORY_WRITER_SYSTEM_PROMPT)
    @UserMessage(MEMORY_WRITER_USER_PROMPT)
    String extract(@V("userMessage") String userMessage, @V("aiResponse") String aiResponse);
}