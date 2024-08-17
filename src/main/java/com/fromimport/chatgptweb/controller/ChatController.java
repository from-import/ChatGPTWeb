package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<String> chat(@RequestBody ChatRequest chatRequest) {
        String message = chatRequest.getMessage();
        Long userId = chatRequest.getUserId();
        log.info("Received message from frontend: {}", message);

        // 保存用户的消息
        chatMessageService.saveChatMessage(userId, message, "user");

        // 处理请求并获取响应
        return openAIService.chatgpt(message)
                .doOnNext(response -> {
                    log.info("Sending response to frontend: {}", response);

                    // 保存 ChatGPT 的回复
                    chatMessageService.saveChatMessage(userId, response, "chatgpt");
                })
                .doOnError(error -> {
                    log.error("Error occurred while processing chat: ", error);
                });
    }
}
