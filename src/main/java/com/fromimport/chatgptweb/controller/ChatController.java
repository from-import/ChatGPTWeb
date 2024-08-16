package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.model.ChatRequest;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {

    private final OpenAIService openAIService;

    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody ChatRequest chatRequest) {
        String message = chatRequest.getMessage();
        log.info("Received message from frontend: {}", message);

        // 处理请求并获取响应
        return openAIService.chatgpt(message)
                .doOnNext(response -> log.info("Sending response to frontend: {}", response));
    }
}
