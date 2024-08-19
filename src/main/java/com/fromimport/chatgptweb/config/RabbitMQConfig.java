package com.fromimport.chatgptweb.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类，用于定义队列、交换机和它们之间的绑定关系。
 * 本配置使用了 Topic Exchange 类型，适用于对消息进行模式匹配的场景。
 */
@Configuration
public class RabbitMQConfig {

    // 定义队列名称常量
    public static final String CHAT_QUEUE = "chatQueue";

    // 定义交换机名称常量
    public static final String CHAT_EXCHANGE = "chatExchange";

    /**
     * 定义一个队列，用于存放聊天相关的消息。
     * @return 返回一个新创建的队列实例，队列名为 "chatQueue"。
     */
    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE);
    }

    /**
     * 定义一个 Topic Exchange 交换机，用于路由聊天相关的消息。
     * Topic Exchange 允许基于模式的路由，例如 "chat.*"。
     * @return 返回一个新创建的 TopicExchange 实例，交换机名为 "chatExchange"。
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    /**
     * 定义一个绑定关系，将 "chatQueue" 队列绑定到 "chatExchange" 交换机上，
     * 并使用路由键模式 "chat.#"。
     * 这意味着以 "chat." 开头的所有消息都将路由到 "chatQueue" 队列中。
     * @param chatQueue     注入的 "chatQueue" 队列实例。
     * @param chatExchange  注入的 "chatExchange" 交换机实例。
     * @return 返回一个新创建的 Binding 实例，表示队列和交换机之间的绑定关系。
     */
    @Bean
    public Binding binding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with("chat.#");
    }
}