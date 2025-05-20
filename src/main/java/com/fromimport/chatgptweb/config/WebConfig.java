package com.fromimport.chatgptweb.config;

import com.fromimport.chatgptweb.common.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")            // 拦截所有路径
                .excludePathPatterns(
                        "/login.html",                // 放行登录页
                        "/register.html",             // 放行注册页
                        "/api/login", "/api/register",// 放行登录/注册接口
                        "/css/**", "/js/**", "/images/**" // 放行静态资源
                );
    }
}
