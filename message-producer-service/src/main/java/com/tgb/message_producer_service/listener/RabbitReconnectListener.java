package com.tgb.message_producer_service.listener;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tgb.message_producer_service.event.RabbitReconnectedEvent;
import com.rabbitmq.client.ShutdownSignalException;

@Component
public class RabbitReconnectListener implements ConnectionListener {

    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean hasRetried = new AtomicBoolean(false);

    public RabbitReconnectListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onCreate(Connection connection) {
        if (hasRetried.compareAndSet(false, true)) {
            System.out.println("RabbitMQ reconnected. Publishing event...");
            eventPublisher.publishEvent(new RabbitReconnectedEvent(this));
        }
    }

    @Override
    public void onShutDown(ShutdownSignalException signal) {
        hasRetried.set(false);
        System.out.println("RabbitMQ connection lost.");
    }
}

