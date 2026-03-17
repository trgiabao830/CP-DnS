package com.tgb.message_producer_service.event;

import org.springframework.context.ApplicationEvent;

public class RabbitReconnectedEvent extends ApplicationEvent {
    public RabbitReconnectedEvent(Object source) {
        super(source);
    }
}
