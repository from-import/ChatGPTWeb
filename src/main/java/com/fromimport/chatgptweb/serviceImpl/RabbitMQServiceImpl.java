package com.fromimport.chatgptweb.serviceImpl;

import com.fromimport.chatgptweb.config.RabbitMQConfig;
import com.fromimport.chatgptweb.service.RabbitMQService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQServiceImpl implements RabbitMQService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange chatExchange;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public void sendMessage(String message) {
        try {
            amqpTemplate.convertAndSend(RabbitMQConfig.CHAT_EXCHANGE, "chat.queue", message);
            log.info("Message 已存储到 RabbitMQ: {}", message);
        } catch (Exception e) {
            log.error("Message 存储到 RabbitMQ 发生异常", e);
        }
    }
}
