package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.User;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.UserService;
import com.yuntian.chat_app.utils.JwtUtil;
import com.yuntian.chat_app.vo.UserLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    //登录接口
    @RequestMapping("/login")
    public Result<UserLoginVo> userLogin(@RequestBody User user){

        log.info("userLogin:{}",user);
        userService.login(user);

        HashMap<String,Object> claim = new HashMap<>();
        claim.put("id",user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl(),claim);
        UserLoginVo loginVo = UserLoginVo.builder()
                .id(user.getId())
                .token(token)
                .build();
        return Result.success(loginVo);

    }
}
