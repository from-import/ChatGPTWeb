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
import java.util.concurrent.TimeUnit;

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
                .eq(Conversation::getUserId, userId) // WHERE user_id = #{userId}
                .isNull(Conversation::getEndTimestamp) // AND end_timestamp IS NULL
                .one(); // LIMIT 1
    }

    @Override
    public boolean save(Conversation conversation) {
        return super.save(conversation);
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithFirstMessage(Long userId) {
        // Redis优化后执行耗时：6ms
        // 原始的MYSQL查询耗时：14ms
        // 2024-08-20 18:07:36.038  INFO 32160 --- [nio-8080-exec-6] c.f.c.aspect.ExecutionTimeAspect         : List com.fromimport.chatgptweb.serviceImpl.ConversationServiceImpl.getConversationHistoryWithFirstMessage(Long) 执行时间: 14 ms
        // 2024-08-20 18:08:28.190  INFO 29168 --- [nio-8080-exec-6] c.f.c.aspect.ExecutionTimeAspect         : List com.fromimport.chatgptweb.serviceImpl.ConversationServiceImpl.getConversationHistoryWithFirstMessage(Long) 执行时间: 6 ms
        log.info("开始获取用户ID为 {} 的对话历史", userId);

        // 先从 Redis 中读取缓存数据
        String conversationsJson = redisTemplate.opsForValue().get("user:conversations:" + userId);

        if (conversationsJson != null && !conversationsJson.trim().isEmpty()) {
            try {
                // 从缓存中读取数据并转换为 List<Map<String, Object>>
                List<Map<String, Object>> cachedConversations = objectMapper.readValue(conversationsJson, List.class);
                log.info("从 Redis 中获取到对话历史数据: {}", cachedConversations);
                return cachedConversations;
            } catch (JsonProcessingException e) {
                log.error("从 Redis 中读取对话历史数据时出错: ", e);
            }
        }

        // 如果缓存中没有数据，从 MySQL 中读取
        log.info("Redis 中没有找到用户ID为 {} 的对话历史数据，从 MySQL 中加载", userId);
        List<Conversation> conversations = this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByAsc(Conversation::getStartTimestamp));

        log.info("用户ID为 {} 的对话数量: {}", userId, conversations.size());

        List<Map<String, Object>> conversationHistory = conversations.stream()
                .map(conversation -> {
                    log.info("正在处理对话ID: {}", conversation.getId());

                    ChatMessage firstMessage = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>() // getOne 由 IService 接口提供，用于从数据库中获取一条符合条件的记录。如果有多条记录满足条件，则返回第一条。
                            .eq(ChatMessage::getConversationId, conversation.getId()) // 筛选出 conversationId 等于 conversation.getId() 的 ChatMessage 记录。
                            .eq(ChatMessage::getSender, "user") // 这个条件进一步筛选出 sender 字段等于 "user" 的记录。
                            .orderByAsc(ChatMessage::getTimestamp) // 这个条件指定了对结果按照 timestamp 字段进行升序排序。
                            .last("LIMIT 1")); // 限制查询结果最多返回一条记录，即使有多个记录满足条件，也只会返回第一条。

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

        // 将数据存储到 Redis 中
        try {
            String conversationsJsonNew = objectMapper.writeValueAsString(conversationHistory);
            redisTemplate.opsForValue().set("user:conversations:" + userId, conversationsJsonNew);

            log.info("用户ID {} 的对话历史已加载到 Redis", userId);
        } catch (JsonProcessingException e) {
            log.error("将对话历史转换为 JSON 时出错: ", e);
        }

        return conversationHistory;
    }

    @Override
    public List<Map<String, Object>> getConversationHistoryWithFirstMessageInMySQL(Long userId) {
        List<Conversation> conversations = this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByAsc(Conversation::getStartTimestamp));

        log.info("用户ID为 {} 的对话数量: {}", userId, conversations.size());

        List<Map<String, Object>> conversationHistory = conversations.stream()
                .map(conversation -> {
                    log.info("正在处理对话ID: {}", conversation.getId());

                    ChatMessage firstMessage = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>() // getOne 由 IService 接口提供，用于从数据库中获取一条符合条件的记录。如果有多条记录满足条件，则返回第一条。
                            .eq(ChatMessage::getConversationId, conversation.getId()) // 筛选出 conversationId 等于 conversation.getId() 的 ChatMessage 记录。
                            .eq(ChatMessage::getSender, "user") // 这个条件进一步筛选出 sender 字段等于 "user" 的记录。
                            .orderByAsc(ChatMessage::getTimestamp) // 这个条件指定了对结果按照 timestamp 字段进行升序排序。
                            .last("LIMIT 1")); // 限制查询结果最多返回一条记录，即使有多个记录满足条件，也只会返回第一条。

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

        // 将数据存储到 Redis 中
        try {
            String conversationsJsonNew = objectMapper.writeValueAsString(conversationHistory);

            // redisTemplate.opsForValue().set("user:conversations:" + userId, conversationsJsonNew);
            String cacheKey = "user:conversations:" + userId;
            redisTemplate.opsForValue().set(cacheKey, conversationsJsonNew);
            // 设置 10 分钟后过期
            redisTemplate.expire(cacheKey, 10, TimeUnit.MINUTES);
            log.info("用户ID {} 的对话历史已加载到 Redis，键 {} 将在 10 分钟后过期", userId, cacheKey);
            log.info("用户ID {} 的对话历史已加载到 Redis", userId);
            /*
            当业务量激增或者 Redis 节点重启、故障时，如果所有缓存都瞬间失效，会引发大量并发请求打到数据库上，造成数据库压力骤增，甚至崩溃。
            通过对各个 key 设置不同或随机的过期时间，可以让缓存失效的时间点错开，缓解“缓存雪崩”风险；同时也可以结合业务写入、对话结束时主动删除 key，进一步平滑失效过程。
            如果不主动删除或过期，Redis 中会不断堆积历史会话列表数据；随着用户量和对话量的增长，内存占用会越来越高。
            为对话历史列表设置合理的生存周期（例如 5–10 分钟），可以让过时的数据自动被垃圾回收，避免占用无谓的内存。
             */

        } catch (JsonProcessingException e) {
            log.error("将对话历史转换为 JSON 时出错: ", e);
        }

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

}