package com.fromimport.chatgptweb.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.fromimport.chatgptweb.common.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化，如有需要可以在此添加初始化逻辑
        logger.info("AuthFilter 初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String token = httpRequest.getHeader("Authorization");

        logger.info("收到请求：URI={}, Authorization头={}", requestURI, token);

        // 检查是否有 JWT，并验证其有效性
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去掉 "Bearer " 前缀

            if (JwtUtils.validateToken(token, JwtUtils.getUsernameFromToken(token))) {
                logger.info("JWT 验证通过：URI={}", requestURI);
                chain.doFilter(request, response); // JWT 验证通过，继续处理请求
                return;
            } else {
                logger.warn("JWT 验证失败：URI={}", requestURI);
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return; // JWT 无效，返回 401 未授权状态
            }
        }

        // 如果是注册、登录相关的请求，直接放行
        if (requestURI.equals("/login.html") || requestURI.equals("/register.html")
                || requestURI.equals("/api/login") || requestURI.equals("/api/register")) {
            logger.info("允许访问公共资源：URI={}", requestURI);
            chain.doFilter(request, response);
        } else {
            logger.warn("未授权访问尝试，重定向到登录页面：URI={}", requestURI);
            httpResponse.sendRedirect("/login.html");
        }
    }

    @Override
    public void destroy() {
        // 过滤器销毁时的资源清理，如有需要可以在此添加清理逻辑
        logger.info("AuthFilter 销毁");
    }
}