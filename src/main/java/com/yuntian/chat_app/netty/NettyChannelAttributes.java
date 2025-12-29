package com.yuntian.chat_app.netty;

import io.netty.util.AttributeKey;

public final class NettyChannelAttributes {

    public static final AttributeKey<Long> GROUP_ID = AttributeKey.valueOf("groupId");
    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");

    private NettyChannelAttributes() {
    }
}

