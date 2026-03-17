package com.tgb.message_producer_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.tgb.message_producer_service.listener.RabbitReconnectListener;

@Configuration
public class RabbitMQConfig {

    @Autowired
    private RabbitReconnectListener reconnectListener;

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String CANCEL_ORDER_QUEUE = "cancel.order.queue";
    public static final String ROUTING_KEY = "order.cancel";

    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String DLX_QUEUE = "dlx.cancel.queue";
    public static final String DLX_ROUTING_KEY = "dlx.order.cancel";

    public static final String OUTBOX_EXCHANGE = "internal.exchange";
    public static final String OUTBOX_QUEUE = "outbox.notify.queue";
    public static final String OUTBOX_ROUTING_KEY = "outbox.notify";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUsername);
        factory.setPassword(rabbitPassword);

        factory.addConnectionListener(reconnectListener);

        return factory;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public TopicExchange exchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Queue cancelOrderQueue() {
        return QueueBuilder.durable(CANCEL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .withArgument("x-message-ttl", 5 * 60 * 1000)
                .build();
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Binding cancelOrderBinding() {
        return BindingBuilder.bind(cancelOrderQueue())
                .to(exchange())
                .with(ROUTING_KEY);
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with(DLX_ROUTING_KEY);
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public TopicExchange outboxExchange() {
        return new TopicExchange(OUTBOX_EXCHANGE);
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Queue outboxNotifyQueue() {
        return QueueBuilder.durable(OUTBOX_QUEUE).build();
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public Binding outboxNotifyBinding() {
        return BindingBuilder.bind(outboxNotifyQueue())
                .to(outboxExchange())
                .with(OUTBOX_ROUTING_KEY);
    }

}
