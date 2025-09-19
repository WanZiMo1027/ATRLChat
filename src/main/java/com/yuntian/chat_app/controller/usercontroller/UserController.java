package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.UserService;
import com.yuntian.chat_app.utils.JwtUtil;
import com.yuntian.chat_app.vo.UserLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController("UserUserController")
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    public Result<UserLoginVo> userLogin(@RequestBody User loginReq) {
        log.info("userLogin: {}", loginReq);

        // 使用 service 返回的数据库用户对象
        User dbUser = userService.login(loginReq);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", dbUser.getId());
        claims.put("username", dbUser.getUsername()); // 可选但推荐，便于前端展示

        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        UserLoginVo loginVo = UserLoginVo.builder()
                .id(dbUser.getId())
                .username(dbUser.getUsername())
                .token(token)
                .build();

        return Result.success(loginVo);
    }

    /**
     * 注册用户
     * @param user
     * @return
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        log.info("register: {}", user);
        boolean register = userService.register(user);
        return Result.success(register);
    }
}