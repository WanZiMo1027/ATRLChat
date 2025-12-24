package com.yuntian.chat_app.service.adminService.adminServiceImpl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yuntian.chat_app.dto.UserPageQueryDTO;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.exception.UserException;
import com.yuntian.chat_app.mapper.adminMapper.AdminUserMapper;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.service.adminService.AdminUserService;
import com.yuntian.chat_app.vo.AdminUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;

    @Override
    public PageResult pageQuery(UserPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<User> page = adminUserMapper.pageQuery(dto.getUsername());

        List<AdminUserVo> list = page.getResult().stream().map(user -> {
            AdminUserVo vo = new AdminUserVo();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult(page.getTotal(), list);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 简单校验是否存在
        User user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new UserException(UserException.USER_NOT_FOUND, "用户不存在");
        }
        adminUserMapper.updateStatus(id, status);
    }
}
