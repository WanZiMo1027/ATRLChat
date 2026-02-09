package com.yuntian.chat_app.service.userService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "customOpenAiChatModel"
)
public interface MemoryWriterService {

    String MEMORY_WRITER_SYSTEM_PROMPT = """
            你是“长期记忆写入器”。
            你的任务是从对话中提取适合长期保存的、可复用的记忆点，并以严格 JSON 输出。
            【提取原则】
            1. **偏好捕获（最高优先级）**：如果用户表达了喜好（如“喜欢吃/喝...”、“讨厌...”），提取其偏好（如“用户喜欢喝水”）。
            2. **用户画像**：提取用户的自称、职业、生活习惯、性格特征。
            3. **事实性信息**：提取对话中提及的客观事实（如“我的幸运数字是42”）。
            【过滤规则】
            - 不要保存单纯的打招呼（如“你好”）。
            - 不要保存纯粹的情绪宣泄（如“我好累”），除非包含原因。
            - 严禁保存大段的AI回复内容，只关注用户侧的信息。
            输出必须是 JSON 数组，数组元素包含字段：type, memory, confidence。
            - type 只能取：PREFERENCE (偏好), PROFILE (画像), GOAL (目标), RULE (规则)。
            - memory：使用第三人称陈述句（如“用户喜欢喝冰美式”）。
            - confidence 0.0 到 1.0 的置信度。
            若无值得保存内容，输出 []。
            不要输出任何解释、不要使用 Markdown 代码块。
            """;

    String MEMORY_WRITER_USER_PROMPT = """
            用户消息：
            {{userMessage}}

            AI 回复：
            {{aiResponse}}
            """;

    @SystemMessage(MEMORY_WRITER_SYSTEM_PROMPT)
    @UserMessage(MEMORY_WRITER_USER_PROMPT)
    String extract(@V("userMessage") String userMessage, @V("aiResponse") String aiResponse);
}

