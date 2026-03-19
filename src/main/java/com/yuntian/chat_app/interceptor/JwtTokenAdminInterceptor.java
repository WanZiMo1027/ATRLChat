package com.yuntian.chat_app.interceptor;

import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.properties.JwtProperties;
import com.yuntian.chat_app.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    private static final String PREFERRED_ADMIN_HEADER = "admin-token";

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String configuredHeader = jwtProperties.getAdminTokenName();
        String token = request.getHeader(PREFERRED_ADMIN_HEADER);
        String usedHeader = PREFERRED_ADMIN_HEADER;

        if (!StringUtils.hasText(token) && StringUtils.hasText(configuredHeader)) {
            token = request.getHeader(configuredHeader);
            usedHeader = configuredHeader;
        }

        log.info("admin jwt check uri={}, preferredHeader={}, usedHeader={}, hasToken={}",
                request.getRequestURI(), PREFERRED_ADMIN_HEADER, usedHeader, StringUtils.hasText(token));

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long adminId = Long.valueOf(claims.get("id").toString());
            BaseContext.setCurrentId(adminId);
            return true;
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
