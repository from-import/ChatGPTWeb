package com.fromimport.chatgptweb.aspect;

import com.fromimport.chatgptweb.annotation.LoadConversationsToRedis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class LoadConversationsAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public LoadConversationsAspect(RedisTemplate<String, String> redisTemplate,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Autowired
    private ConversationService conversationService;

    @AfterReturning(pointcut = "@annotation(loadConversationsToRedis)", returning = "response")
    public void afterLogin(Object response, LoadConversationsToRedis loadConversationsToRedis) throws JsonProcessingException {
        if (response instanceof ResponseEntity) {
            log.info("用户登录成功，开始尝试通过afterLogin加载对话到 Redis");
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
            Object body = responseEntity.getBody();

            if (body instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) body;

                // 从响应中获取 userId，假设 userId 存储在响应中
                String userIdStr = (String) responseMap.get("userId");
                Long userId = userIdStr != null ? Long.valueOf(userIdStr) : null;

                if (userId != null) {
                    log.info("用户ID {} 登录成功，开始加载对话到 Redis", userId);
                    // 获取用户的对话历史
                    List<Map<String, Object>> conversations = conversationService.getConversationHistoryWithFirstMessageInMySQL(userId);
                    // 将对话历史转换为 JSON 字符串
                    String conversationsJson = objectMapper.writeValueAsString(conversations);
                    log.info("用户ID {} 的对话历史数据: {}", userId, conversationsJson);

                    // 存储到 Redis 中，使用适当的 key
                    redisTemplate.opsForValue().set("user:conversations:" + userId, conversationsJson);
                    log.info("用户ID {} 的对话历史已加载到 Redis", userId);

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