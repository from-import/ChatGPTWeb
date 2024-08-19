package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ChatMessageMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@Transactional
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper; // 注入 ChatMessageMapper


    @Override
    public Mono<Void> saveChatMessage(Long userId, Long conversationId, String message, String sender) {
        try {
            // 创建一个新的 ChatMessage 实例
            ChatMessage chatMessage = new ChatMessage();
            // 设置聊天消息的属性
            chatMessage.setUserId(userId);  // 设置用户 ID
            chatMessage.setConversationId(conversationId);  // 设置对话 ID
            chatMessage.setContent(message);  // 设置消息内容
            chatMessage.setSender(sender);  // 设置消息发送者
            chatMessage.setTimestamp(LocalDateTime.now());  // 设置消息时间戳为当前时间

            // 使用 MyBatis-Plus 的 chatMessageMapper 将消息插入到数据库中
            chatMessageMapper.insert(chatMessage);

        } catch (Exception e) {
            // 捕捉并记录可能发生的异常
            log.error("Error saving chat message", e);
        }
        // 由于没有异步操作，这里直接返回 Mono.empty() 以表示方法已完成
        return Mono.empty();
    }
}