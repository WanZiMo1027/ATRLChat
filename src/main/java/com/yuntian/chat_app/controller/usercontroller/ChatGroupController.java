package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.dto.GetMessagesRequestDTO;
import com.yuntian.chat_app.dto.GroupChatMessageDTO;
import com.yuntian.chat_app.dto.JoinGroupRequestDTO;
import com.yuntian.chat_app.dto.LeaveGroupRequestDTO;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.entity.ChatGroup;
import com.yuntian.chat_app.entity.ChatGroupMember;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.mapper.userMapper.UserMapper;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.ChatGroupMemberService;
import com.yuntian.chat_app.service.userService.ChatGroupMessageService;
import com.yuntian.chat_app.service.userService.ChatGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
@Tag(name = "群聊接口", description = "群组创建、加入、成员、资料和消息查询接口")
public class ChatGroupController {


    private final ChatGroupService groupService;


    private final ChatGroupMemberService memberService;


    private final ChatGroupMessageService messageService;

    private final CharacterService characterService;

    private final UserMapper userMapper;

    /**
     * 创建群组
     */
    @PostMapping("/create")
    @Operation(summary = "创建群组", description = "创建一个新的群聊群组")
    public Result<Long> createGroup(@RequestBody ChatGroup request) {


        Long groupId = groupService.createGroup(request.getCreatorId(), request.getName(), request.getCharacterId(), request.getDescription());
        return Result.success(groupId);
    }

    /**
     * 加入群组
     */
    @PostMapping("/join")
    @Operation(summary = "加入群组", description = "用户加入指定群组")
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
    @Operation(summary = "查询群成员", description = "查询指定群组的成员列表")
    public Result<List<ChatGroupMember>> getMembers(@PathVariable Long groupId) {
        List<ChatGroupMember> members = memberService.getGroupMembers(groupId);
        return Result.success(members);
    }

    /**
     * 查询群详情（用于群资料页）
     */
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

    /**
     * 修改群头像
     */
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

    /**
     * 按群号查询群信息（用于通过群号搜索群）
     */
    @GetMapping("/{groupId}")
    @Operation(summary = "按群号查询群信息", description = "根据群组 ID 查询群组基础信息")
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
    @Operation(summary = "查询我的群组", description = "查询当前用户已加入的群组列表")
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
    @Operation(summary = "查询群历史消息", description = "通过路径参数和分页参数查询群组历史消息")
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
    @Operation(summary = "复杂查询群历史消息", description = "通过请求体查询群组历史消息")
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
    @Operation(summary = "退出群组", description = "用户退出指定群组")
    public Result<Void> leaveGroup(@RequestBody LeaveGroupRequestDTO request) {
        boolean success = memberService.leaveGroup(
                request.getGroupId(),
                request.getUserId()
        );
        return success ? Result.success() : Result.error("退出失败");
    }
}
