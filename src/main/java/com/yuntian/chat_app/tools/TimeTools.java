package com.yuntian.chat_app.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTools {

    private static final DateTimeFormatter DEFAULT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool("获取当前服务器本地时间，格式为 yyyy-MM-dd HH:mm:ss")
    public String now() {
        return LocalDateTime.now().format(DEFAULT_FMT);
    }
}
