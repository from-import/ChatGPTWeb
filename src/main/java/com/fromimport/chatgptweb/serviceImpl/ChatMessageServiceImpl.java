package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ChatMessageMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public void saveChatMessage(Long userId, Long conversationId, String message, String sender) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(message);
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);
    }
}
