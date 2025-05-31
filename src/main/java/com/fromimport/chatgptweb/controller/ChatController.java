package com.fromimport.chatgptweb.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.config.RabbitMQConfig;
import com.fromimport.chatgptweb.entity.Conversation;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.ConversationService;
import com.fromimport.chatgptweb.service.OpenAIService;
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
        // @RequestBody 注解的作用是将请求体中的 JSON 数据转换为 ChatRequest 对象。
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        User user = (User) httpRequest.getSession().getAttribute("user");
        Long userId = user != null ? user.getId() : null;

        String message = chatRequest.getMessage();
        if (userId == null) {
            return Mono.error(new RuntimeException("用户未登录或会话过期"));
        }

        // 获取或创建对话 (Conversation)
        Conversation conversation = getOrCreateConversation(userId);

        // 保存用户的消息
        chatMessageService.saveChatMessage(userId, conversation.getId(), message, "user");

        // 保证后续读取到的对话历史是最新的。因为在用户发送新消息、并将消息写入数据库之后，
        // 原来缓存中保存的“对话历史”已经过时了，如果不主动清除，下次有人去读这个缓存就会拿到旧的数据。通过在写操作后立即调用
        // 就能在下一次读取时触发“缓存未命中”，从数据库重新加载最新的对话列表并回写到 Redis，这样就既利用了缓存提速，又避免了脏数据的风险。
        String historyKey = "user:conversations:" + userId;
        redisTemplate.delete(historyKey);

        // 构造消息
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("conversationId", conversation.getId().toString());
        payload.put("message", message);

        // 将消息发布到 RabbitMQ 队列
        String jsonPayload = new ObjectMapper().writeValueAsString(payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "chat.payload", jsonPayload);

        // 返回一个用于查询的ID或其他信息
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversation.getId().toString()); // 确保 conversationId 为字符串
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
        User user = (User) httpRequest.getSession().getAttribute("user");
        Long userId = user != null ? user.getId() : null;
        if (userId == null) {
            return Mono.error(new RuntimeException("用户未登录或会话过期"));
        }

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