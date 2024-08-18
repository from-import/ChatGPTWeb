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

import com.fromimport.chatgptweb.entity.User;
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
        User user = (User) httpRequest.getSession().getAttribute("user");

        logger.info("收到请求：URI={}, 用户登录状态={}", requestURI, user != null ? "已登录" : "未登录");

        if (user != null || requestURI.equals("/login.html") || requestURI.equals("/register.html")
                || requestURI.equals("/api/login")) {
            logger.info("允许访问：URI={}", requestURI);
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