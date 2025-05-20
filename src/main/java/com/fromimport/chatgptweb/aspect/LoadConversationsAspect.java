package com.fromimport.chatgptweb.aspect;

import com.fromimport.chatgptweb.annotation.LoadConversationsToRedis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class LoadConversationsAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public LoadConversationsAspect(RedisTemplate<String, String> redisTemplate,
                                   ObjectMapper objectMapper,
                                   RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
    }

    @Autowired
    private ConversationService conversationService;

    @AfterReturning(pointcut = "@annotation(loadConversationsToRedis)", returning = "response")
    public void afterLogin(Object response, LoadConversationsToRedis loadConversationsToRedis) throws JsonProcessingException {
        if (!(response instanceof ResponseEntity<?>)) {
            log.error("响应不是 ResponseEntity 类型: {}", response);
            return;
        }
        ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
        Object body = responseEntity.getBody();
        if (!(body instanceof Map)) {
            log.error("响应体不是 Map 类型: {}", body);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) body;
        String userIdStr = (String) responseMap.get("userId");
        if (userIdStr == null) {
            log.warn("登录响应中未找到 userId");
            return;
        }
        Long userId = Long.valueOf(userIdStr);
        String cacheKey = "user:conversations:" + userId;

        RLock lock = redissonClient.getLock("lock:conversation:load:" + userId);
        lock.lock();
        try {
            log.info("获取到锁 lock:conversation:load:{}, 开始加载对话历史到 Redis", userId);
            List<Map<String, Object>> conversations =
                    conversationService.getConversationHistoryWithFirstMessageInMySQL(userId);
            String conversationsJson = objectMapper.writeValueAsString(conversations);
            // 写入 Redis 并设置 10 分钟自动过期
            redisTemplate.opsForValue().set(cacheKey, conversationsJson, 10, TimeUnit.MINUTES);
            log.info("用户ID {} 的对话历史已加载到 Redis（键 {}，10 分钟后过期）", userId, cacheKey);
        } finally {
            lock.unlock();
            log.info("释放锁 lock:conversation:load:{}", userId);
        }
    }

}