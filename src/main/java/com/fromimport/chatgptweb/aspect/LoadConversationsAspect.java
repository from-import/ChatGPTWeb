package com.fromimport.chatgptweb.aspect;

import com.fromimport.chatgptweb.annotation.LoadConversationsToRedis;
import com.fromimport.chatgptweb.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@Slf4j
public class LoadConversationsAspect {

    private final ConversationService conversationService;

    @Autowired
    public LoadConversationsAspect(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @AfterReturning(pointcut = "@annotation(loadConversationsToRedis)", returning = "response")
    public void afterLogin(Object response, LoadConversationsToRedis loadConversationsToRedis) {
        if (response instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
            Object body = responseEntity.getBody();

            if (body instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) body;

                // 从响应中获取 userId，假设 userId 存储在响应中
                String userIdStr = (String) responseMap.get("userId");
                Long userId = userIdStr != null ? Long.valueOf(userIdStr) : null;

                if (userId != null) {
                    log.info("用户ID {} 登录成功，开始加载对话到 Redis", userId);
                    conversationService.loadUserConversationsToRedis(userId);
                } else {
                    log.warn("登录响应中未找到 userId");
                }
            } else {
                log.error("响应体不是 Map 类型: {}", body);
            }
        } else {
            log.error("响应不是 ResponseEntity 类型: {}", response);
        }
    }
}
