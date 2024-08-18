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
        // 获取用户的所有对话
        List<Conversation> conversations = this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByAsc(Conversation::getStartTimestamp));

        // 存储对话历史的结果
        List<Map<String, Object>> conversationHistory = conversations.stream()
                .map(conversation -> {
                    // 使用 ChatMessageService 获取对话的第一条用户消息
                    ChatMessage firstMessage = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conversation.getId())
                            .eq(ChatMessage::getSender, "user")
                            .orderByAsc(ChatMessage::getTimestamp)
                            .last("LIMIT 1"));

                    // 创建一个 HashMap 以确保类型匹配
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("conversationId", conversation.getId());
                    resultMap.put("startTimestamp", conversation.getStartTimestamp());
                    resultMap.put("firstMessage", firstMessage != null ? firstMessage.getContent() : "");

                    return resultMap;
                })
                .collect(Collectors.toList());

        // 返回存储的对话历史结果
        return conversationHistory;
    }
}