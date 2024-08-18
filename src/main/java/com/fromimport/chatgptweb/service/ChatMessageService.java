package com.fromimport.chatgptweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fromimport.chatgptweb.entity.ChatMessage;

public interface ChatMessageService extends IService<ChatMessage> {
    void saveChatMessage(Long userId, Long conversationId, String message, String sender);
}
