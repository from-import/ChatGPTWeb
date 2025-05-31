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
        // 防止同一条消息被重复保存到数据库中
        String dedupKey = "chatmsg:d:" + userId + ":" + conversationId + ":" + message.hashCode();
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(inserted)) {
            log.warn("重复消息忽略：{}", message);
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setConversationId(conversationId);
        chatMessage.setContent(message);
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessageMapper.insert(chatMessage);
    }

    /**
     * 同步版保存 ChatMessage，并在写库后清理相关缓存
     */
    @Override
    public void saveChatMessage(Long userId, Long conversationId, String message, String sender) {
        String lockKey = "lock:user:conversations:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            // 最多等待 5 秒去抢锁，抢到后 10 秒自动解锁
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("【saveChatMessage】无法获取分布式锁={}, 退化为直接写库且不清除缓存", lockKey);
                insertMessage(userId, conversationId, message, sender);
                return;
            }

            log.info("【saveChatMessage】加锁成功 => userId={}, conversationId={}, sender={}",
                    userId, conversationId, sender);

            // 写库
            insertMessage(userId, conversationId, message, sender);

            // 写库后再删除缓存
            String historyKey = "user:conversations:" + userId;
            redisTemplate.delete(historyKey);
            log.info("【saveChatMessage】清除用户对话历史缓存 => {}", historyKey);

            String responseKey = "chat_response:" + conversationId;
            redisTemplate.delete(responseKey);
            log.info("【saveChatMessage】清除对话响应缓存 => {}", responseKey);

            // 如果恰好此时有大量并发读取请求过来，所有请求都会同时走到数据库，极易造成「雪崩」
            // 解决方案： 在删除缓存后，马上把最新的数据从数据库中查出，然后写回到 Redis。这样，在真正的业务读请求到来前，缓存已被及时补齐，避免瞬时并发落到数据库。

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【saveChatMessage】获取分布式锁 {} 时被中断", lockKey, e);
            // 退化为无锁写库
            insertMessage(userId, conversationId, message, sender);

        } catch (Exception e) {
            log.error("【saveChatMessage】写库或清缓存时发生异常", e);

        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("【saveChatMessage】释放锁 => {}", lockKey);
            }
        }
    }
}