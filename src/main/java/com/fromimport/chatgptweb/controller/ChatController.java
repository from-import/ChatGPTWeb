package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.entity.Conversation;
import com.fromimport.chatgptweb.entity.User;
import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.ConversationService;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    private final OpenAIService openAIService;
    private final ChatMessageService chatMessageService;

    public ChatController(OpenAIService openAIService, ChatMessageService chatMessageService) {
        this.openAIService = openAIService;
        this.chatMessageService = chatMessageService;
    }

    @Autowired
    private ConversationService conversationService;

    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody ChatRequest chatRequest, ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        User user = (User) httpRequest.getSession().getAttribute("user");
        log.info("收到请求：URI={}, 用户登录状态={}", requestURI, user != null ? "已登录" : "未登录");
        Long userId = user != null ? user.getId() : null; // 获取 userId
        log.info("获取当前用户信息为： {}", userId);

        String message = chatRequest.getMessage();
        log.info("从前端获取到了用户发送的内容： {}", message);
        if (userId == null) {
            return Mono.error(new RuntimeException("用户未登录或会话过期"));
        }

        // 获取或创建对话 (Conversation)
        Conversation conversation = getOrCreateConversation(userId);

        // 保存用户的消息
        chatMessageService.saveChatMessage(userId, conversation.getId(), message, "user");

        // 处理请求并获取响应
        return openAIService.chatgpt(message)
                .flatMap(response -> {
                    // 保存 ChatGPT 的回复
                    chatMessageService.saveChatMessage(userId, conversation.getId(), response, "chatgpt");
                    log.info("已保存ChatGPT返回的内容: {}", response);

                    // 返回响应
                    return Mono.just(response);
                });
    }

    @PostMapping("/endConversation")
    public Mono<String> endConversation(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        User user = (User) httpRequest.getSession().getAttribute("user");
        Long userId = user != null ? user.getId() : null; // 获取 userId
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
        // 此处假设我们使用了 ConversationService 来处理对话逻辑
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