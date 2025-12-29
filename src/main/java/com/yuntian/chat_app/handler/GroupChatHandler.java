package com.yuntian.chat_app.handler;

import com.alibaba.fastjson2.JSON;
import com.yuntian.chat_app.context.MonitorContext;
import com.yuntian.chat_app.context.MonitorContextHolder;
import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.netty.NettyGroupManager;
import com.yuntian.chat_app.netty.NettyChannelAttributes;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import com.yuntian.chat_app.service.userService.ConsultantService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@ChannelHandler.Sharable
public class GroupChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final ConcurrentHashMap<Long, Semaphore> GROUP_AI_SEMAPHORE = new ConcurrentHashMap<>();

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private NettyGroupManager groupManager;

    // 注入新的Service
    @Autowired
    private ChatGroupMessageService messageService;

    @Autowired
    private ChatGroupService groupService;

    @Autowired
    private CharacterMapper characterMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        String text = frame.text();
        GroupChatMessageDTO msg = JSON.parseObject(text, GroupChatMessageDTO.class);

        Long groupId = ctx.channel().attr(NettyChannelAttributes.GROUP_ID).get();
        Long userId = ctx.channel().attr(NettyChannelAttributes.USER_ID).get();
        String username = ctx.channel().attr(NettyChannelAttributes.USERNAME).get();
        if (groupId == null || userId == null) {
            ctx.close();
            return;
        }

        if (msg.getType() == 1) {
            ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(Result.success(true))));

        } else if (msg.getType() == 2) {
            // 类型2: 聊天消息

            ChatGroup group = groupService.getGroupById(groupId);
            if (group == null || group.getCharacterId() == null) {
                ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(Result.error("群组未绑定智能体"))));
                return;
            }
            Long characterId = group.getCharacterId();

            // 1. 保存消息到数据库
            msg.setGroupId(groupId);
            msg.setUserId(userId);
            msg.setCharacterId(characterId);
            msg.setSenderType("USER");
            if (username != null && !username.isBlank()) {
                msg.setSenderName(username);
            }
            Long messageId = messageService.saveMessage(msg);
            msg.setMessageId(messageId);
            msg.setTimestamp(System.currentTimeMillis());

            // 2. 广播用户消息
            groupManager.broadcast(groupId,
                    new TextWebSocketFrame(JSON.toJSONString(msg)));

            // 3. 判断是否触发 @AI
            if (shouldTriggerAI(msg.getContent())) {
                handleAIMessage(groupId, characterId, username, msg);
            }
        } else if (msg.getType() == 3) {
            groupManager.removeChannel(ctx.channel());
            ctx.close();
        }
    }

    private boolean shouldTriggerAI(String content) {
        return content != null && (content.contains("@AI") || content.contains("@ai"));
    }

    private void handleAIMessage(Long groupId, Long characterId, String username, GroupChatMessageDTO userMsg) {
        CompletableFuture.runAsync(() -> {
            Semaphore semaphore = GROUP_AI_SEMAPHORE.computeIfAbsent(groupId, k -> new Semaphore(1));
            semaphore.acquireUninterruptibly();
            try {
                // 1. 查询角色信息
                Character character = characterMapper.selectById(characterId);
                if (character == null) {
                    log.warn("角色不存在: {}", characterId);
                    return;
                }

                // 2. 构建 MemoryId
                String memoryId = "group_chat:" + groupId + ":char:" + characterId;

                // 3. 清理消息内容
                String cleanMessage = userMsg.getContent()
                        .replaceAll("@(AI|ai)", "").trim();
                if (username != null && !username.isBlank()) {
                    cleanMessage = username + ": " + cleanMessage;
                } else {
                    cleanMessage = "user-" + userMsg.getUserId() + ": " + cleanMessage;
                }

                MonitorContext monitorContext = MonitorContext.builder()
                        .userId(String.valueOf(userMsg.getUserId()))
                        .characterId(String.valueOf(characterId))
                        .memoryId(memoryId)
                        .build();
                MonitorContextHolder.setContext(monitorContext);

                // 4. 调用 AI 服务
                String aiResponse = consultantService.chat(
                        memoryId,
                        cleanMessage,
                        character.getName(),
                        character.getAppearance(),
                        character.getBackground(),
                        character.getPersonality(),
                        character.getClassicLines()
                );

                // 5. 构建AI消息
                GroupChatMessageDTO aiMsg = GroupChatMessageDTO.builder()
                        .type(2)
                        .groupId(groupId)
                        .userId(userMsg.getUserId())
                        .characterId(characterId)
                        .content(aiResponse)
                        .contentType("text")
                        .senderType("AI")
                        .senderName(character.getName())
                        .timestamp(System.currentTimeMillis())
                        .build();

                // 6. 保存AI消息
                Long messageId = messageService.saveMessage(aiMsg);
                aiMsg.setMessageId(messageId);

                // 7. 广播AI回复
                groupManager.broadcast(groupId,
                        new TextWebSocketFrame(JSON.toJSONString(aiMsg)));

            } catch (Exception e) {
                log.error("AI处理异常", e);
            } finally {
                MonitorContextHolder.clearContext();
                semaphore.release();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        groupManager.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Netty error", cause);
        ctx.close();
    }
}
