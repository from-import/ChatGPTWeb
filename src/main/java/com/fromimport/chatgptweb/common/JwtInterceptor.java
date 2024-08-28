package com.fromimport.chatgptweb.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀3
            log.info("请求中包含 JWT 令牌: {}", token);
            if (JwtUtils.validateToken(token, JwtUtils.getUsernameFromToken(token))) {
                log.info("JWT 令牌验证通过: {}", token);
                return true; // Token 验证通过
            } else {
                log.warn("JWT 令牌验证失败: {}", token);
            }
        } else {
            log.warn("请求中未找到 JWT 令牌");
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false; // Token 验证失败
    }


}