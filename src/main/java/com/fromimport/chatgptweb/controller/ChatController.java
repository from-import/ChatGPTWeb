package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpSession;

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

    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody ChatRequest chatRequest, HttpSession session) {
        String message = chatRequest.getMessage();
        Long userId = (Long) session.getAttribute("userId"); // 从 Session 中获取用户 ID
        log.info("获取当前用户信息为： {}", userId);
        log.info("从前端获取到了用户发送的内容： {}", message);

        if (userId == null) {
            return Mono.error(new RuntimeException("用户未登录或会话过期"));
        }

        // 保存用户的消息
        chatMessageService.saveChatMessage(userId, message, "user");

        // 处理请求并获取响应
        return openAIService.chatgpt(message)
                .flatMap(response -> {
                    // 保存 ChatGPT 的回复
                    chatMessageService.saveChatMessage(userId, response, "chatgpt");
                    log.info("已保存ChatGPT返回的内容: {}", response);

                    // 返回响应
                    return Mono.just(response);
                });
    }
}