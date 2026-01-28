package com.yuntian.chat_app.netty;

import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import com.yuntian.chat_app.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class NettyWebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    private final JwtProperties jwtProperties;
    private final ChatGroupMemberService memberService;
    private final UserMapper userMapper;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest request)) {
            super.channelRead(ctx, msg);
            return;
        }

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = decoder.parameters();

        String groupIdStr = first(params, "groupId");
        if (groupIdStr == null) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.BAD_REQUEST, "missing groupId");
            return;
        }

        Long groupId;
        try {
            groupId = Long.valueOf(groupIdStr);
        } catch (Exception e) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.BAD_REQUEST, "invalid groupId");
            return;
        }

        String tokenParamName = jwtProperties.getUserTokenName();
        String token = first(params, tokenParamName);
        if (token == null && !"token".equalsIgnoreCase(tokenParamName)) {
            token = first(params, "token");
        }
        if (token == null) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.UNAUTHORIZED, "missing token");
            return;
        }

        Claims claims;
        try {
            claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
        } catch (Exception e) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.UNAUTHORIZED, "invalid token");
            return;
        }

        Object idObj = claims.get("id");
        if (idObj == null) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.UNAUTHORIZED, "invalid token claims");
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(idObj));
        } catch (Exception e) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.UNAUTHORIZED, "invalid token claims");
            return;
        }

        if (!memberService.isMemberInGroup(groupId, userId)) {
            ReferenceCountUtil.release(msg);
            writeAndClose(ctx, HttpResponseStatus.FORBIDDEN, "not a group member");
            return;
        }

        String username = null;
        Object usernameObj = claims.get("username");
        if (usernameObj != null) {
            username = String.valueOf(usernameObj);
        }

        ctx.channel().attr(NettyChannelAttributes.GROUP_ID).set(groupId);
        ctx.channel().attr(NettyChannelAttributes.USER_ID).set(userId);
        if (username != null && !username.isBlank()) {
            ctx.channel().attr(NettyChannelAttributes.USERNAME).set(username);
        }

        User user = userMapper.selectById(userId);
        if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            ctx.channel().attr(NettyChannelAttributes.AVATAR_URL).set(user.getAvatarUrl());
        }

        request.setUri(decoder.path());

        ctx.pipeline().remove(this);
        ctx.fireChannelRead(msg);
    }

    private static String first(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private static void writeAndClose(ChannelHandlerContext ctx, HttpResponseStatus status, String text) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(text, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
