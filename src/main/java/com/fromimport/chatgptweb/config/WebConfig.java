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
                .addPathPatterns("/api/**") // 只拦截 /api/** 路径下的请求
                .excludePathPatterns(
                        "/api/login",     // 放行登录接口
                        "/api/register"   // 放行注册接口
                );
    }
}