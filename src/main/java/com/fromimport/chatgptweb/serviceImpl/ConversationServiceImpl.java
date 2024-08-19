package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.entity.Conversation;
import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ConversationMapper;
import com.fromimport.chatgptweb.service.ConversationService;
import com.fromimport.chatgptweb.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ConversationServiceImpl(ChatMessageService chatMessageService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.chatMessageService = chatMessageService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Conversation getOngoingConversation(Long userId) {
        return lambdaQuery()
                .eq(Conversation::getUserId, userId)
                .isNull(Conversation::getEndTimestamp)
                .one();
    }

    @Override
    public boolean save(Conversation conversation) {
        return super.save(conversation);
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithFirstMessage(Long userId) {
        log.info("开始获取用户ID为 {} 的对话历史", userId);

        List<Conversation> conversations = this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByAsc(Conversation::getStartTimestamp));

        log.info("用户ID为 {} 的对话数量: {}", userId, conversations.size());

        List<Map<String, Object>> conversationHistory = conversations.stream()
                .map(conversation -> {
                    log.info("正在处理对话ID: {}", conversation.getId());

                    ChatMessage firstMessage = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conversation.getId())
                            .eq(ChatMessage::getSender, "user")
                            .orderByAsc(ChatMessage::getTimestamp)
                            .last("LIMIT 1"));

                    log.info("对话ID {} 的第一条用户消息: {}", conversation.getId(),
                            firstMessage != null ? firstMessage.getContent() : "未找到消息");

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("conversationId", conversation.getId().toString());
                    resultMap.put("startTimestamp", conversation.getStartTimestamp());
                    resultMap.put("firstMessage", firstMessage != null ? firstMessage.getContent() : "");

                    return resultMap;
                })
                .collect(Collectors.toList());

        log.info("对话历史数据: {}", conversationHistory);

        return conversationHistory;
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithConversationId(String conversationId) {
        log.info("开始获取对话ID为 {} 的详细记录", conversationId);

        List<ChatMessage> chatMessages = chatMessageService.list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getTimestamp));

        log.info("对话ID {} 的消息数量: {}", conversationId, chatMessages.size());

        List<Map<String, Object>> conversationDetail = chatMessages.stream()
                .map(chatMessage -> {
                    log.info("处理消息ID: {}", chatMessage.getId());

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("sender", chatMessage.getSender());
                    resultMap.put("text", chatMessage.getContent());
                    resultMap.put("timestamp", chatMessage.getTimestamp());

                    return resultMap;
                })
                .collect(Collectors.toList());

        log.info("对话详细记录数据: {}", conversationDetail);

        return conversationDetail;
    }

    @Override
    public void loadUserConversationsToRedis(Long userId) {
        try {
            // 获取用户的对话历史
            List<Map<String, Object>> conversations = this.getConversationHistoryWithFirstMessage(userId);

            // 将对话历史转换为 JSON 字符串
            String conversationsJson = objectMapper.writeValueAsString(conversations);

            // 存储到 Redis 中，使用适当的 key
            redisTemplate.opsForValue().set("user:conversations:" + userId, conversationsJson);

            log.info("用户ID {} 的对话历史已加载到 Redis", userId);
        } catch (JsonProcessingException e) {
            log.error("将对话历史转换为 JSON 时出错: ", e);
        }
    }
}