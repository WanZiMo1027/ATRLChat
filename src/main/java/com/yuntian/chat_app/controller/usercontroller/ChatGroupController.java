package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.dto.GetMessagesRequestDTO;
import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.dto.GroupJoinApprovalUpdateDTO;
import com.yuntian.chat_app.dto.GroupPublicUpdateDTO;
import com.yuntian.chat_app.dto.JoinGroupRequestDTO;
import com.yuntian.chat_app.dto.LeaveGroupRequestDTO;
import com.yuntian.chat_app.dto.RejectJoinRequestDTO;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import com.yuntian.chat_app.vo.ChatGroupJoinRequestVo;
import com.yuntian.chat_app.vo.JoinGroupResultVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
@Tag(name = "群聊接口", description = "群组创建、加入、成员、资料、大厅和消息查询接口")
public class ChatGroupController {

    private final ChatGroupService groupService;

    private final ChatGroupMemberService memberService;

    private final ChatGroupMessageService messageService;

    private final CharacterService characterService;

    private final UserMapper userMapper;

    @PostMapping("/create")
    @Operation(summary = "创建群组", description = "创建一个新的群聊群组")
    public Result<Long> createGroup(@RequestBody ChatGroup request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }

        Long groupId = groupService.createGroup(
                currentUserId,
                request.getName(),
                request.getCharacterId(),
                request.getDescription(),
                request.getIsPublic(),
                request.getJoinRequiresApproval()
        );
        return Result.success(groupId);
    }

    @GetMapping("/hall")
    @Operation(summary = "查询群聊大厅", description = "分页查询公开展示到群聊大厅的群组")
    public Result<PageResult> getGroupHall(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword) {
        Long currentUserId = BaseContext.getCurrentId();
        return Result.success(groupService.getHallGroups(page, size, keyword, currentUserId));
    }

    @PostMapping("/join")
    @Operation(summary = "加入群组", description = "当前用户加入群组；开启审核时创建待审核申请")
    public Result<JoinGroupResultVo> joinGroup(@RequestBody JoinGroupRequestDTO request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        return Result.success(memberService.joinGroup(
                request.getGroupId(),
                currentUserId,
                request.getNickname()
        ));
    }

    @PostMapping("/{groupId}/public")
    @Operation(summary = "设置群组大厅展示", description = "群主或管理员设置群组是否公开展示到群聊大厅")
    public Result<Integer> updateGroupPublic(@PathVariable Long groupId,
                                             @RequestBody GroupPublicUpdateDTO request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        if (request == null || request.getPublicVisible() == null) {
            return Result.error("isPublic不能为空");
        }

        Integer value = request.getPublicVisible() ? 1 : 0;
        groupService.updateGroupPublic(groupId, currentUserId, value);
        return Result.success(value);
    }

    @PostMapping("/{groupId}/join-approval")
    @Operation(summary = "设置入群审核", description = "群主或管理员设置用户加入群聊是否需要管理员同意")
    public Result<Integer> updateJoinApproval(@PathVariable Long groupId,
                                              @RequestBody GroupJoinApprovalUpdateDTO request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        if (request == null || request.getJoinRequiresApproval() == null) {
            return Result.error("joinRequiresApproval不能为空");
        }

        Integer value = request.getJoinRequiresApproval() ? 1 : 0;
        groupService.updateJoinRequiresApproval(groupId, currentUserId, value);
        return Result.success(value);
    }

    @GetMapping("/{groupId}/join-requests")
    @Operation(summary = "查询入群申请", description = "群主或管理员查询指定群组的入群申请")
    public Result<List<ChatGroupJoinRequestVo>> getJoinRequests(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "PENDING") String status) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        return Result.success(memberService.getJoinRequests(groupId, status, currentUserId));
    }

    @PostMapping("/join-requests/{requestId}/approve")
    @Operation(summary = "通过入群申请", description = "群主或管理员通过待审核入群申请")
    public Result<Void> approveJoinRequest(@PathVariable Long requestId) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        return memberService.approveJoinRequest(requestId, currentUserId)
                ? Result.success()
                : Result.error("入群申请状态已变化");
    }

    @PostMapping("/join-requests/{requestId}/reject")
    @Operation(summary = "拒绝入群申请", description = "群主或管理员拒绝待审核入群申请")
    public Result<Void> rejectJoinRequest(@PathVariable Long requestId,
                                          @RequestBody(required = false) RejectJoinRequestDTO request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        String reason = request == null ? null : request.getReason();
        return memberService.rejectJoinRequest(requestId, currentUserId, reason)
                ? Result.success()
                : Result.error("入群申请状态已变化");
    }

    @GetMapping("/{groupId}/online-count")
    @Operation(summary = "查询群组在线人数", description = "查询指定群组当前 WebSocket 在线人数")
    public Result<Integer> getOnlineCount(@PathVariable Long groupId) {
        return Result.success(groupService.getOnlineCount(groupId));
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "查询群成员", description = "查询指定群组的成员列表")
    public Result<List<ChatGroupMember>> getMembers(@PathVariable Long groupId) {
        List<ChatGroupMember> members = memberService.getGroupMembers(groupId);
        return Result.success(members);
    }

    @GetMapping("/{groupId}/detail")
    @Operation(summary = "查询群详情", description = "查询群组、创建人、角色和成员数等详情")
    public Result<Map<String, Object>> getGroupDetail(@PathVariable Long groupId) {
        ChatGroup group = groupService.getGroupById(groupId);
        if (group == null) {
            return Result.error("群不存在");
        }

        User creator = null;
        if (group.getCreatorId() != null) {
            creator = userMapper.selectById(group.getCreatorId());
        }
        Character character = null;
        if (group.getCharacterId() != null) {
            character = characterService.getCharacterById(group.getCharacterId());
        }
        int memberCount = memberService.countGroupMembers(groupId);

        Map<String, Object> data = new HashMap<>();
        data.put("group", group);
        data.put("memberCount", memberCount);
        if (creator != null) {
            Map<String, Object> creatorInfo = new HashMap<>();
            creatorInfo.put("id", creator.getId());
            creatorInfo.put("username", creator.getUsername());
            creatorInfo.put("avatarUrl", creator.getAvatarUrl());
            data.put("creator", creatorInfo);
        } else {
            data.put("creator", null);
        }
        if (character != null) {
            Map<String, Object> characterInfo = new HashMap<>();
            characterInfo.put("id", character.getId());
            characterInfo.put("name", character.getName());
            characterInfo.put("image", character.getImage());
            data.put("character", characterInfo);
        } else {
            data.put("character", null);
        }
        return Result.success(data);
    }

    @PostMapping("/{groupId}/avatar")
    @Operation(summary = "更新群头像", description = "上传并更新指定群组头像")
    public Result<String> updateGroupAvatar(@PathVariable Long groupId,
                                            @RequestParam("file") MultipartFile file) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        return Result.success(groupService.updateGroupAvatar(groupId, currentUserId, file));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "按群号查询群信息", description = "根据群组 ID 查询群组基础信息")
    public Result<ChatGroup> getGroup(@PathVariable Long groupId) {
        ChatGroup group = groupService.getGroupById(groupId);
        if (group == null) {
            return Result.error("群不存在");
        }
        return Result.success(group);
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的群组", description = "查询当前用户已加入的群组列表")
    public Result<List<ChatGroup>> getMyGroups() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("未登录");
        }
        return Result.success(groupService.getGroupsByUser(userId));
    }

    @GetMapping("/{groupId}/messages")
    @Operation(summary = "查询群历史消息", description = "通过路径参数和分页参数查询群组历史消息")
    public Result<List<GroupChatMessageDTO>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<GroupChatMessageDTO> messages = messageService.getGroupMessages(groupId, page, size);
        return Result.success(messages);
    }

    @PostMapping("/messages/query")
    @Operation(summary = "复杂查询群历史消息", description = "通过请求体查询群组历史消息")
    public Result<List<GroupChatMessageDTO>> queryMessages(@RequestBody GetMessagesRequestDTO request) {
        List<GroupChatMessageDTO> messages = messageService.getGroupMessages(
                request.getGroupId(),
                request.getPage(),
                request.getSize()
        );
        return Result.success(messages);
    }

    @PostMapping("/leave")
    @Operation(summary = "退出群组", description = "当前用户退出指定群组")
    public Result<Void> leaveGroup(@RequestBody LeaveGroupRequestDTO request) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Result.error("未登录");
        }
        boolean success = memberService.leaveGroup(
                request.getGroupId(),
                currentUserId
        );
        return success ? Result.success() : Result.error("退出失败");
    }
}
