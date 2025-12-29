package com.yuntian.chat_app.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class NettyWebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {

    private final NettyGroupManager groupManager;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            Long groupId = ctx.channel().attr(NettyChannelAttributes.GROUP_ID).get();
            if (groupId != null) {
                groupManager.addChannel(groupId, ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}

