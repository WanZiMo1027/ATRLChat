package com.yuntian.chat_app.vo;

import lombok.Data;

@Data
public class ChatGroupHallItemVo {
    private Long groupId;
    private String name;
    private String avatarUrl;
    private String description;
    private Integer memberCount;
    private Integer maxMembers;
    private Integer onlineCount;
    private Boolean isMember;
    private String joinRequestStatus;
    private Integer joinRequiresApproval;
}
