package com.fromimport.chatgptweb.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.common.JwtUtils;
import com.fromimport.chatgptweb.config.RabbitMQConfig;
import com.fromimport.chatgptweb.entity.Conversation;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.ConversationService;
import com.fromimport.chatgptweb.service.OpenAIService;
import com.fromimport.chatgptweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    @Autowired
    private OpenAIService openAIService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private RabbitTemplate rabbitTemplate; // 注入 RabbitTemplate
    @Autowired
    private StringRedisTemplate redisTemplate; // 注入 RedisTemplate
    @Autowired // 注入Redisson
    private RedissonClient redissonClient;
    @Autowired
    private UserService userService;




    /*
    // 原本的采用Session登录的@GetMapping
    // 现在替换为JWT
    @GetMapping("/session/userId")
    public ResponseEntity<Map<String, Long>> getUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        Map<String, Long> response = new HashMap<>();
        response.put("userId", user != null ? user.getId() : null);
        return ResponseEntity.ok(response);
    }
     */

    @PostMapping("/chat")
    public Mono<Map<String, Object>> chat(@RequestBody ChatRequest chatRequest, ServletRequest request) throws JsonProcessingException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 从请求头中获取 JWT
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Mono.error(new RuntimeException("缺少或非法的 Authorization 令牌"));
        }

        token = token.substring(7).trim(); // 去掉 "Bearer "

        // 提取用户名
        String username;
        try {
            username = JwtUtils.getUsernameFromToken(token);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("非法令牌或已过期"));
        }

        // 根据用户名查找用户
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Mono.error(new RuntimeException("用户不存在"));
        }
        Long userId = user.getId();

        String message = chatRequest.getMessage();

        // 获取或创建对话 (Conversation)
        Conversation conversation = getOrCreateConversation(userId);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, conversation.getId(), message, "user");

        // 清除对话历史缓存（强制刷新）
        String historyKey = "user:conversations:" + userId;
        redisTemplate.delete(historyKey);

        // 构造 RabbitMQ 消息
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("conversationId", conversation.getId().toString());
        payload.put("message", message);
        String jsonPayload = new ObjectMapper().writeValueAsString(payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "chat.payload", jsonPayload);

        // 响应返回
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversation.getId().toString());
        response.put("message", "消息已发送，正在处理中");
        return Mono.just(response);
    }


    @GetMapping("/chat/{conversationId}")
    public Mono<Map<String, Object>> getChatResponse(@PathVariable String conversationId) {
        // @PathVariable 注解用于将 URL 中的路径变量映射到方法参数上。
        // 在这个例子中，路径变量 {conversationId} 的值被映射到 conversationId 参数。
        Long conversationIdLong = Long.parseLong(conversationId);

        // 从 Redis 中获取聊天记录
        String response = (String) redisTemplate.opsForValue().get("chat_response_" + conversationIdLong);

        Map<String, Object> result = new HashMap<>();
        if (response == null) {
            result.put("status", "pending");
        } else {
            result.put("status", "completed");
            result.put("response", response);
            // 清除 Redis 缓存
            redisTemplate.delete("chat_response_" + conversationIdLong);
            log.info("已清除 Redis 缓存: chat_response_{}", conversationIdLong);
        }
        return Mono.just(result);
    }

    @PostMapping("/endConversation")
    public Mono<String> endConversation(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. 解析 JWT Token
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Mono.error(new RuntimeException("缺少或非法的 Authorization 令牌"));
        }
        token = token.substring(7).trim(); // 去掉 "Bearer "

        // 2. 提取用户名
        String username;
        try {
            username = JwtUtils.getUsernameFromToken(token);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("非法令牌或已过期"));
        }

        // 3. 获取用户 ID
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Mono.error(new RuntimeException("用户不存在"));
        }
        Long userId = user.getId();

        // 4. 分布式锁处理对话结束逻辑
        RLock lock = redissonClient.getLock("lock:conversation:end:" + userId);
        lock.lock();
        try {
            Conversation conv = conversationService.getOngoingConversation(userId);
            if (conv != null) {
                conv.setEndTimestamp(LocalDateTime.now());
                conversationService.updateById(conv);
                redisTemplate.delete("user:conversations:" + userId);
                return Mono.just("对话已结束");
            }
            return Mono.just("没有进行中的对话");
        } finally {
            lock.unlock();
        }
    }

    public Conversation getOrCreateConversation(Long userId) {
        RLock lock = redissonClient.getLock("lock:conversation:create:" + userId);
        lock.lock();
        try {
            Conversation conv = conversationService.getOngoingConversation(userId);
            if (conv == null) {
                conv = new Conversation();
                conv.setUserId(userId);
                conv.setStartTimestamp(LocalDateTime.now());
                conversationService.save(conv);
                // 写库后立即删除老缓存，让下次读走库并回填
                redisTemplate.delete("user:conversations:" + userId);
            }
            return conv;
        } finally {
            lock.unlock();
        }
    }
}