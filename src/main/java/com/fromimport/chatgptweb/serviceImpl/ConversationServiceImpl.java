package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fromimport.chatgptweb.entity.Conversation;
import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ConversationMapper;
import com.fromimport.chatgptweb.service.ConversationService;
import com.fromimport.chatgptweb.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    private final ChatMessageService chatMessageService;

    public ConversationServiceImpl(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Override
    public Conversation getOngoingConversation(Long userId) {
        // 使用 lambdaQuery 查询是否有未结束的对话
        return lambdaQuery()
                .eq(Conversation::getUserId, userId)
                .isNull(Conversation::getEndTimestamp)
                .one();
    }

    @Override
    public boolean save(Conversation conversation) {
        // 直接使用 ServiceImpl 的 save 方法保存对话
        return super.save(conversation);
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithFirstMessage(Long userId) {
        log.info("开始获取用户ID为 {} 的对话历史", userId);

        // 获取用户的所有对话
        List<Conversation> conversations = this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByAsc(Conversation::getStartTimestamp));

        log.info("用户ID为 {} 的对话数量: {}", userId, conversations.size());

        // 存储对话历史的结果
        List<Map<String, Object>> conversationHistory = conversations.stream()
                .map(conversation -> {
                    log.info("正在处理对话ID: {}", conversation.getId());

                    // 使用 ChatMessageService 获取对话的第一条用户消息
                    ChatMessage firstMessage = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conversation.getId())
                            .eq(ChatMessage::getSender, "user")
                            .orderByAsc(ChatMessage::getTimestamp)
                            .last("LIMIT 1"));

                    log.info("对话ID {} 的第一条用户消息: {}", conversation.getId(),
                            firstMessage != null ? firstMessage.getContent() : "未找到消息");

                    // 创建一个 HashMap 以确保类型匹配
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("conversationId", conversation.getId());
                    resultMap.put("startTimestamp", conversation.getStartTimestamp());
                    resultMap.put("firstMessage", firstMessage != null ? firstMessage.getContent() : "");

                    return resultMap;
                })
                .collect(Collectors.toList());

        // 使用 log.info 打印对话历史数据
        log.info("对话历史数据: {}", conversationHistory);

        // 返回存储的对话历史结果
        return conversationHistory;
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithConversationId(Long conversationId) {
        log.info("开始获取对话ID为 {} 的详细记录", conversationId);

        // 获取该对话的所有消息
        List<ChatMessage> chatMessages = chatMessageService.list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getTimestamp));

        log.info("对话ID {} 的消息数量: {}", conversationId, chatMessages.size());

        // 创建存储消息的结果列表
        List<Map<String, Object>> conversationDetail = chatMessages.stream()
                .map(chatMessage -> {
                    log.info("处理消息ID: {}", chatMessage.getId());

                    // 创建一个 HashMap 以确保类型匹配
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("sender", chatMessage.getSender()); // 发送者 (user 或 bot)
                    resultMap.put("text", chatMessage.getContent());  // 消息内容
                    resultMap.put("timestamp", chatMessage.getTimestamp()); // 消息时间戳

                    return resultMap;
                })
                .collect(Collectors.toList());

        // 使用 log.info 打印对话详细记录数据
        log.info("对话详细记录数据: {}", conversationDetail);

        // 返回存储的详细对话记录结果
        return conversationDetail;
    }
}