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

    @GetMapping("/session/userId")
    public ResponseEntity<Map<String, Long>> getUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        Map<String, Long> response = new HashMap<>();
        response.put("userId", user != null ? user.getId() : null);
        return ResponseEntity.ok(response);
    }

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

        // 获取正在进行的对话
        Conversation conversation = conversationService.getOngoingConversation(userId);
        if (conversation != null) {
            conversation.setEndTimestamp(LocalDateTime.now());
            conversationService.updateById(conversation); // 更新对话的结束时间
            log.info("对话 {} 已结束", conversation.getId());
            return Mono.just("对话已结束");
        } else {
            return Mono.just("没有进行中的对话");
        }
    }

    private Conversation getOrCreateConversation(Long userId) {
        // 查询数据库中是否有一个尚未结束的对话
        Conversation conversation = conversationService.getOngoingConversation(userId);
        if (conversation == null) {
            // 如果没有找到尚未结束的对话，创建一个新的对话
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setStartTimestamp(LocalDateTime.now());
            conversationService.save(conversation); // 保存新的对话到数据库
        }
        return conversation;
    }
}