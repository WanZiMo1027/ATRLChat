package com.yuntian.chat_app.controller.admincontroller;

import com.yuntian.chat_app.dto.UserPageQueryDTO;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.adminService.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user")
@Slf4j
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 用户分页查询
     * @param dto
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(UserPageQueryDTO dto) {
        log.info("admin user page: {}", dto);
        PageResult result = adminUserService.pageQuery(dto);
        return Result.success(result);
    }

    /**
     * 封禁/解封用户
     * @param id
     * @param status 0:正常, 1:封禁
     * @return
     */
    @PostMapping("/status/{status}")
    public Result status(@RequestParam Long id, @PathVariable Integer status) {
        log.info("admin update user status: id={}, status={}", id, status);
        adminUserService.updateStatus(id, status);
        return Result.success();
    }
}
