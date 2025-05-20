package com.fromimport.chatgptweb.service;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface OpenAIService {
    Mono<String> chatgpt(String message);

    /**
     * 调用 AnythingLLM 的 /v1/workspace/{slug}/chat 接口
     * @param workspaceSlug 工作空间的唯一 slug
     * @param message       用户输入的消息
     * @param mode          调用模式，例如 "query"
     * @return Mono<String> 原始 JSON 响应体
     */
    Mono<String> workspaceChat(String workspaceSlug, String message, String mode);

}