package com.fromimport.chatgptweb.common;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    // 白名单：不需要 JWT 的请求
    private static final Set<String> WHITELIST = Set.of(
            "/",              // 根路径
            "/index.html",    // 欢迎页面
            "/login.html",
            "/register.html",
            "/api/login",
            "/api/register",
            "/css/",
            "/js/",
            "/images/"
    );

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String uri = req.getRequestURI();
        log.info("请求 URI = {}", uri);

        // 放行白名单
        for (String path : WHITELIST) {
            if (uri.equals(path) || uri.startsWith(path)) {
                log.debug("白名单放行：{}", uri);
                return true;
            }
        }

        // JWT 验证（同之前逻辑）
        String header = req.getHeader("Authorization");
        boolean valid = false;
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String user = JwtUtils.getUsernameFromToken(token);
                valid = JwtUtils.validateToken(token, user);
            } catch (ExpiredJwtException | MalformedJwtException | IllegalArgumentException e) {
                log.warn("JWT 验证异常: {}", e.getMessage());
            }
        }

        if (valid) {
            return true;
        }

        // 区分 AJAX vs 浏览器导航
        boolean isAjax = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
        if (isAjax) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            resp.sendRedirect("/login.html");
        }
        return false;
    }

}
