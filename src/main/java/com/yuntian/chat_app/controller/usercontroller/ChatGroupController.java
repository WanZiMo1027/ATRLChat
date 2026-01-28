package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.dto.GetMessagesRequestDTO;
import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.dto.JoinGroupRequestDTO;
import com.yuntian.chat_app.dto.LeaveGroupRequestDTO;
import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class ChatGroupController {


    private final ChatGroupService groupService;


    private final ChatGroupMemberService memberService;


    private final ChatGroupMessageService messageService;

    private final UserMapper userMapper;

    /**
     * 创建群组
     */
    @PostMapping("/create")
    public Result<Long> createGroup(@RequestBody ChatGroup request) {


        Long groupId = groupService.createGroup(request.getCreatorId(), request.getName(), request.getCharacterId(), request.getDescription());
        return Result.success(groupId);
    }

    /**
     * 加入群组
     */
    @PostMapping("/join")
    public Result<Void> joinGroup(@RequestBody JoinGroupRequestDTO request) {
        boolean success = memberService.joinGroup(
                request.getGroupId(),
                request.getUserId(),
                request.getNickname()
        );
        return success ? Result.success() : Result.error("加入失败");
    }

    /**
     * 查询群成员
     */
    @GetMapping("/{groupId}/members")
    public Result<List<ChatGroupMember>> getMembers(@PathVariable Long groupId) {
        List<ChatGroupMember> members = memberService.getGroupMembers(groupId);
        return Result.success(members);
    }

    /**
     * 查询群详情（用于群资料页）
     */
    @GetMapping("/{groupId}/detail")
    public Result<Map<String, Object>> getGroupDetail(@PathVariable Long groupId) {
        ChatGroup group = groupService.getGroupById(groupId);
        if (group == null) {
            return Result.error("群不存在");
        }

        User creator = null;
        if (group.getCreatorId() != null) {
            creator = userMapper.selectById(group.getCreatorId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("group", group);
        if (creator != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("id", creator.getId());
            creatorInfo.put("username", creator.getUsername());
            creatorInfo.put("avatarUrl", creator.getAvatarUrl());
            data.put("creator", creatorInfo);
        } else {
            data.put("creator", null);
        }
        return Result.success(data);
    }

    /**
     * 按群号查询群信息（用于通过群号搜索群）
     */
    @GetMapping("/{groupId}")
    public Result<ChatGroup> getGroup(@PathVariable Long groupId) {
        ChatGroup group = groupService.getGroupById(groupId);
        if (group == null) {
            return Result.error("群不存在");
        }
        return Result.success(group);
    }

    /**
     * 查询当前用户加入的群组（用于选择进入哪个群）
     */
    @GetMapping("/my")
    public Result<List<ChatGroup>> getMyGroups() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        return Result.success(groupService.getGroupsByUser(userId));
    }

    /**
     * 查询历史消息（方式一：路径参数 + 查询参数，保持原样）
     */
    @GetMapping("/{groupId}/messages")
    public Result<List<GroupChatMessageDTO>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<GroupChatMessageDTO> messages = messageService.getGroupMessages(groupId, page, size);
        return Result.success(messages);
    }

    /**
     * 查询历史消息（方式二：POST + RequestBody，推荐用于复杂查询）
     * 如果需要更多查询条件（如时间范围、关键词搜索等），用这种方式
     */
    @PostMapping("/messages/query")
    public Result<List<GroupChatMessageDTO>> queryMessages(@RequestBody GetMessagesRequestDTO request) {
        List<GroupChatMessageDTO> messages = messageService.getGroupMessages(
                request.getGroupId(),
                request.getPage(),
                request.getSize()
        );
        return Result.success(messages);
    }

    /**
     * 退出群组
     */
    @PostMapping("/leave")
    public Result<Void> leaveGroup(@RequestBody LeaveGroupRequestDTO request) {
        boolean success = memberService.leaveGroup(
                request.getGroupId(),
                request.getUserId()
        );
        return success ? Result.success() : Result.error("退出失败");
    }
}
