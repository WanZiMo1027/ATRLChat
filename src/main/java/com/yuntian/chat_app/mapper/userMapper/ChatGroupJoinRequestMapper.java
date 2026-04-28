package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.entity.ChatGroupJoinRequest;
import com.yuntian.chat_app.vo.ChatGroupJoinRequestVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatGroupJoinRequestMapper {

    int insert(ChatGroupJoinRequest request);

    ChatGroupJoinRequest selectById(@Param("id") Long id);

    ChatGroupJoinRequest selectPendingByGroupIdAndUserId(@Param("groupId") Long groupId,
                                                         @Param("userId") Long userId);

    List<ChatGroupJoinRequestVo> selectByGroupIdAndStatus(@Param("groupId") Long groupId,
                                                          @Param("status") String status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("reviewerId") Long reviewerId,
                     @Param("rejectReason") String rejectReason);
}
