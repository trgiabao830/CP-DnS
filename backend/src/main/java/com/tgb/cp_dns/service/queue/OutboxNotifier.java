package com.tgb.cp_dns.service.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxNotifier {

    private final RabbitTemplate rabbitTemplate;

    private static final String OUTBOX_EXCHANGE = "internal.exchange";
    private static final String OUTBOX_ROUTING_KEY = "outbox.notify";

    public void notifyNewOutboxMessage() {
        try {
            String signal = "NEW_DATA";
            rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_ROUTING_KEY, signal);
            System.out.println("Sent Outbox Notification Signal.");
        } catch (Exception e) {
            System.err.println("Failed to send Outbox Notification: " + e.getMessage());
        }
    }
}
