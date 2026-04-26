package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.dto.UserInfoDTO;
import com.yuntian.chat_app.dto.UserProfileUpdateDTO;
import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.UserService;
import com.yuntian.chat_app.utils.JwtUtil;
import com.yuntian.chat_app.vo.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController("UserUserController")
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户接口", description = "用户登录、注册、资料与头像接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户登录信息生成用户 JWT")
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
    @Operation(summary = "用户注册", description = "创建普通用户账号")
    public Result<Boolean> register(@RequestBody User user) {
        log.info("register: {}", user);
        boolean register = userService.register(user);
        return Result.success(register);
    }

    /**
     * 查找用户信息
     * @return
     */
    @GetMapping("/user/UserInfo")
    @Operation(summary = "获取当前用户信息", description = "根据 JWT 上下文获取当前登录用户资料")
    public Result<UserInfoDTO> getUserInfo() {
        Long id = BaseContext.getCurrentId();
        //通过threadLocal获取当前登录用户的id
        User user = userService.getById(id);
        return Result.success(UserInfoDTO.from(user));
    }

    /**
     * 修改用户信息
     * @param userProfileUpdateDTO
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "更新当前用户信息", description = "修改当前登录用户的个人资料")
    public Result<Boolean> update(@RequestBody UserProfileUpdateDTO userProfileUpdateDTO) {
        Long currentUserId = BaseContext.getCurrentId();
        log.info("update current user info: userId={}, payload={}", currentUserId, userProfileUpdateDTO);
        boolean update = userService.update(currentUserId, userProfileUpdateDTO);
        return Result.success(update);
    }


    /**
     * 用户上传头像
     */
    @PostMapping("/updateAvatar")
    @Operation(summary = "更新用户头像地址", description = "保存当前用户头像 URL")
    public Result<Boolean> updateUserAvatar(@RequestParam String imageUrl) {
        Long currentUserId = BaseContext.getCurrentId();
        log.info("updateUserAvatar: userId={}, imageUrl={}", currentUserId, imageUrl);
        userService.updateUserAvatar(currentUserId, imageUrl);
        return Result.success(true);
    }

    /**
     * 查看用户头像
     * @return
     */
    @GetMapping("/avatar")
    @Operation(summary = "获取当前用户头像", description = "查询当前登录用户头像 URL")
    public Result<String> getUserAvatar() {
        String avatarUrl = userService.getUserAvatar();
        return Result.success(avatarUrl);
    }

}
