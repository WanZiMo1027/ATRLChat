package com.yuntian.chat_app.netty;

import com.yuntian.chat_app.handler.GroupChatHandler;
import com.yuntian.chat_app.properties.NettyWebSocketProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NettyWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyWebSocketProperties properties;
    private final NettyWebSocketAuthHandler authHandler;
    private final NettyWebSocketHandshakeHandler handshakeHandler;
    private final GroupChatHandler groupChatHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(properties.getMaxContentLength()))
                .addLast(new ChunkedWriteHandler())
                .addLast(authHandler)
                .addLast(new WebSocketServerProtocolHandler(properties.getPath(), null, true))
                .addLast(handshakeHandler)
                .addLast(groupChatHandler);
    }
}
