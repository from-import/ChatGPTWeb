package com.fromimport.chatgptweb.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.OpenAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @RabbitListener(queues = "chatQueue")
    public void receiveMessage(String payload) {
        log.info("收到 RabbitMQ 消息: {}", payload);

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

                            // 将响应存储到 Redis
                            redisTemplate.opsForValue().set("chat_response_" + conversationId, response);
                            log.info("将响应存储到 Redis: {}", response);
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