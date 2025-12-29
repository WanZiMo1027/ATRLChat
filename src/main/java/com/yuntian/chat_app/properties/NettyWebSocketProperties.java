package com.yuntian.chat_app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chatapp.netty.websocket")
@Data
public class NettyWebSocketProperties {

    private boolean enabled = true;
    private String host = "0.0.0.0";
    private int port = 8090;
    private String path = "/ws/group";
    private int maxContentLength = 65536;
}

