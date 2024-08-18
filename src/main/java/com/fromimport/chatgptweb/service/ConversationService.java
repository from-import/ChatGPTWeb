package com.fromimport.chatgptweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fromimport.chatgptweb.entity.Conversation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface ConversationService extends IService<Conversation> {
    Conversation getOngoingConversation(Long userId);
    List<Map<String, Object>> getConversationHistoryWithFirstMessage(Long userId);
}