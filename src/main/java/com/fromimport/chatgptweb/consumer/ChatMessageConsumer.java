package com.fromimport.chatgptweb.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class ChatMessageConsumer {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     *     为何使用多线程：
     *     多线程可以同时处理多个 RabbitMQ 消息，而不是顺序处理。这意味着当多个消息到达队列时，任务会被并行执行，提升消息处理速度，减少处理时间。
     *     线程池通过创建多个线程来同时执行任务，避免了单线程情况下每次处理完一个消息后才能处理下一个的问题，从而提高系统的并发能力。
     *
     *     任务异步化：
     *     通过将消息处理任务提交到线程池 (taskExecutor.submit)，主线程不会被阻塞，立即返回并继续监听队列中的新消息。这样即使某个任务耗时较长，也不会阻塞其他任务的执行。
     * @param payload
     */
    @RabbitListener(queues = "chatQueue")
    public void receiveMessage(String payload) {
        log.info("收到 RabbitMQ 消息: {}", payload);
        long startTime = System.currentTimeMillis();

        // 提交消息处理任务到线程池
        Future<?> future = taskExecutor.submit(() -> {
            try {
                // 从 payload 中提取数据并进行类型转换
                Map<String, Object> data = new ObjectMapper().readValue(payload, Map.class);
                String userId = (String) data.get("userId");
                String conversationId = (String) data.get("conversationId");
                String message = (String) data.get("message");

                log.info("提取的用户 ID: {}", userId);
                log.info("提取的对话 ID: {}", conversationId);
                log.info("提取的消息内容: {}", message);

                // 调用 OpenAI 服务获取响应
                openAIService.chatgpt(message)
                        .subscribe(response -> {
                            // 保存 ChatGPT 响应
                            chatMessageService.saveChatMessage(Long.parseLong(userId), Long.parseLong(conversationId), response, "chatgpt");
                            log.info("从 ChatGPT 获取的响应: {}", response);
                            long endTime = System.currentTimeMillis();
                            log.info("有线程池调度的运行时间: {} ms", (endTime - startTime));

                            // 将响应存储到 Redis
                            redisTemplate.opsForValue().set("chat_response_" + conversationId, response);
                            log.info("将响应存储到 Redis: {}", response);

                            // 通过 WebSocket 推送消息给前端
                            messagingTemplate.convertAndSend("/topic/chat/" + userId, response);
                            log.info("消息通过 WebSocket 推送给用户: {}", userId);
                        });
            } catch (NumberFormatException e) {
                log.error("数值格式转换失败: {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("处理消息失败: {}", e.getMessage(), e);
            }
        });

        // 记录线程池任务的提交
        log.info("提交任务到线程池处理消息: {}", payload);
        try {
            // 等待线程池任务完成，设置超时避免长时间等待
            future.get(30, TimeUnit.SECONDS);
            log.info("线程池任务成功完成: {}", payload);
        } catch (TimeoutException e) {
            log.error("线程池任务超时未完成: {}", e.getMessage(), e);
            // 可以采取进一步的处理措施，如重试任务或记录监控信息
        } catch (Exception e) {
            log.error("等待线程池任务完成失败: {}", e.getMessage(), e);
        }
    }
}