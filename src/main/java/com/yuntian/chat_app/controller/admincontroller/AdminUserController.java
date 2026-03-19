package com.yuntian.chat_app.controller.admincontroller;

import com.yuntian.chat_app.dto.AdminUserStatusUpdateDTO;
import com.yuntian.chat_app.dto.UserPageQueryDTO;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.adminService.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 用户分页查询
     * @param dto
     * @return
     */
    @GetMapping("/user/page")
    public Result<PageResult> page(UserPageQueryDTO dto) {
        validatePageQuery(dto);
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
    @Deprecated
    @PostMapping("/user/status/{status}")
    public Result status(@RequestParam Long id, @PathVariable Integer status) {
        validateStatusPayload(id, status);
        log.info("admin update user status: id={}, status={}", id, status);
        adminUserService.updateStatus(id, status);
        return Result.success();
    }

    @PatchMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody AdminUserStatusUpdateDTO request) {
        Integer status = request == null ? null : request.getStatus();
        validateStatusPayload(id, status);
        log.info("admin patch user status: id={}, status={}", id, status);
        adminUserService.updateStatus(id, status);
        return Result.success();
    }

    private void validatePageQuery(UserPageQueryDTO dto) {
        if (dto == null || dto.getPage() == null || dto.getPage() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        if (dto.getPageSize() == null || dto.getPageSize() < 1 || dto.getPageSize() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageSize must be between 1 and 100");
        }
    }

    private void validateStatusPayload(Long id, Integer status) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be a positive number");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be 0 or 1");
        }
    }
}
