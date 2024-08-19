package com.fromimport.chatgptweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fromimport.chatgptweb.entity.ChatMessage;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface ChatMessageService extends IService<ChatMessage> {
    Mono<Void> saveChatMessage(Long userId, Long conversationId, String message, String sender);
}