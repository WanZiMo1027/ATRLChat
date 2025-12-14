package com.yuntian.chat_app.controller.admincontroller;

import com.yuntian.chat_app.entity.Admin;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.adminService.AdminService;
import com.yuntian.chat_app.utils.JwtUtil;
import com.yuntian.chat_app.vo.AdminLoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final JwtProperties jwtProperties;


    /**
     * 管理员登录
     * @param loginReq
     * @return
     */
    @PostMapping("/login")
    public Result<AdminLoginVo> login(@RequestBody Admin loginReq) {
        log.info("admin login: {}", loginReq);

        // 使用 service 返回的数据库用户对象
        Admin dbAdmin = adminService.login(loginReq);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", dbAdmin.getId());
        claims.put("adminName", dbAdmin.getAdminName());

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        AdminLoginVo loginVo = AdminLoginVo.builder()
                .id(dbAdmin.getId())
                .adminName(dbAdmin.getAdminName())
                .token(token)
                .build();

        return Result.success(loginVo);
    }

    /**
     * 管理员注册
     * @param admin
     * @return
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody Admin admin) {
        log.info("admin register: {}", admin);
        Integer register = adminService.register(admin);
        return Result.success(register != 0);
    }
}
