package com.yuntian.chat_app.netty;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 群组管理器
 * 负责管理群组和 WebSocket 连接的映射关系
 */
@Slf4j
@Component
public class NettyGroupManager {

    // 核心数据结构：groupId -> ChannelGroup
    // ConcurrentHashMap 保证线程安全
    private static final ConcurrentHashMap<Long, ChannelGroup> GROUP_MAP = new ConcurrentHashMap<>();

    // Channel -> groupId 的反向映射（用于连接断开时清理）
    private static final ConcurrentHashMap<Channel, Long> CHANNEL_GROUP_MAP = new ConcurrentHashMap<>();

    /**
     * 将用户的 Channel 加入群组
     * @param groupId 群组ID
     * @param channel 用户的 WebSocket 连接
     */
    public void addChannel(Long groupId, Channel channel) {
        // 1. 获取或创建 ChannelGroup
        ChannelGroup channelGroup = GROUP_MAP.computeIfAbsent(groupId,
                k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));

        // 2. 将 Channel 加入群组
        channelGroup.add(channel);

        // 3. 记录反向映射
        CHANNEL_GROUP_MAP.put(channel, groupId);

        log.info("Channel {} 加入群组 {}, 当前群组人数: {}",
                channel.id().asShortText(), groupId, channelGroup.size());
    }

    /**
     * 从群组移除 Channel（通常在连接断开时调用）
     * @param channel 要移除的连接
     */
    public void removeChannel(Channel channel) {
        Long groupId = CHANNEL_GROUP_MAP.remove(channel);
        if (groupId != null) {
            ChannelGroup channelGroup = GROUP_MAP.get(groupId);
            if (channelGroup != null) {
                channelGroup.remove(channel);
                log.info("Channel {} 离开群组 {}, 剩余人数: {}",
                        channel.id().asShortText(), groupId, channelGroup.size());

                // 如果群组没人了，清理 ChannelGroup
                if (channelGroup.isEmpty()) {
                    GROUP_MAP.remove(groupId);
                    log.info("群组 {} 已清空", groupId);
                }
            }
        }
    }

    /**
     * 广播消息到指定群组的所有在线成员
     * @param groupId 群组ID
     * @param message 消息内容（通常是 TextWebSocketFrame）
     */
    public void broadcast(Long groupId, Object message) {
        ChannelGroup channelGroup = GROUP_MAP.get(groupId);
        if (channelGroup != null && !channelGroup.isEmpty()) {
            channelGroup.writeAndFlush(message);
            log.debug("广播消息到群组 {}, 在线人数: {}", groupId, channelGroup.size());
        } else {
            log.warn("群组 {} 无在线成员，无法广播", groupId);
        }
    }

    /**
     * 向指定群组发送文本消息（便捷方法）
     * @param groupId 群组ID
     * @param text 文本内容
     */
    public void broadcastText(Long groupId, String text) {
        broadcast(groupId, new TextWebSocketFrame(text));
    }

    /**
     * 获取群组的在线人数
     * @param groupId 群组ID
     * @return 在线人数
     */
    public int getOnlineCount(Long groupId) {
        ChannelGroup channelGroup = GROUP_MAP.get(groupId);
        return channelGroup == null ? 0 : channelGroup.size();
    }

    /**
     * 检查某个用户是否在线（通过 Channel 判断）
     * @param channel 用户的 Channel
     * @return 是否在线
     */
    public boolean isOnline(Channel channel) {
        return channel != null && channel.isActive();
    }

    /**
     * 关闭指定群组的所有连接（解散群组时使用）
     * @param groupId 群组ID
     */
    public void closeGroup(Long groupId) {
        ChannelGroup channelGroup = GROUP_MAP.remove(groupId);
        if (channelGroup != null) {
            channelGroup.close(); // 关闭所有 Channel
            log.info("群组 {} 已解散，关闭了 {} 个连接", groupId, channelGroup.size());
        }
    }
}