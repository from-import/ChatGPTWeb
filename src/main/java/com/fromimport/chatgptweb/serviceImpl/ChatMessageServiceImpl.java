package com.fromimport.chatgptweb.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ChatMessageMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Transactional
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired // 注入 ChatMessageMapper
    private ChatMessageMapper chatMessageMapper;

    @Autowired // 注入 Redis 模板
    private StringRedisTemplate redisTemplate;

    @Autowired // 注入Redisson
    private RedissonClient redissonClient;


    /**
     * 执行真正的消息写库操作
     */
    private void insertMessage(Long userId, Long conversationId, String message, String sender) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(message);
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);
    }

    @Override
    public Mono<Void> saveChatMessage(Long userId, Long conversationId, String message, String sender) {
        String lockKey = "lock:user:conversations:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 最多等待 5 秒去拿锁，拿到锁后 10 秒自动解锁
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("无法获取分布式锁 {}，直接写库但不清理缓存", lockKey);
                // 仍然写库，但跳过缓存删除
                insertMessage(userId, conversationId, message, sender);
                return Mono.empty();
            }
            // 加锁成功后，先写库
            insertMessage(userId, conversationId, message, sender);

            // 写库后再删除各类相关缓存
            String historyKey = "user:conversations:" + userId;
            redisTemplate.delete(historyKey);
            log.info("插入消息后清除用户对话历史缓存: {}", historyKey);

            String responseKey = "chat_response:" + conversationId;
            redisTemplate.delete(responseKey);
            log.info("插入消息后清除对话响应缓存: {}", responseKey);

            return Mono.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁 {} 过程中被中断", lockKey, e);
            // 退化为无锁写库
            insertMessage(userId, conversationId, message, sender);
            return Mono.empty();
        } catch (Exception e) {
            log.error("保存消息并清理缓存时发生异常", e);
            return Mono.empty();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}