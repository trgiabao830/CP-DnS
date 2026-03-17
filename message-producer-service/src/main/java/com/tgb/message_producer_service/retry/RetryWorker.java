package com.tgb.message_producer_service.retry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tgb.message_producer_service.config.RabbitMQConfig;
import com.tgb.message_producer_service.event.RabbitReconnectedEvent;
import com.tgb.message_producer_service.model.OutboxMessage;
import com.tgb.message_producer_service.repository.OutboxMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RetryWorker {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxMessageRepository outboxMessageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RetryWorker(RabbitTemplate rabbitTemplate,
                       OutboxMessageRepository outboxMessageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxMessageRepository = outboxMessageRepository;
    }

    @EventListener
    public void onRabbitReconnected(RabbitReconnectedEvent event) {
        System.out.println("Detected RabbitMQ reconnected, triggering retry...");
        retryUnsentMessagesOnce();
    }

    public void retryUnsentMessagesOnce() {
        System.out.println("RetryWorker running due to previous message failure...");

        List<OutboxMessage> unsentMessages = entityManager
                .createQuery("SELECT o FROM OutboxMessage o WHERE o.sent = false", OutboxMessage.class)
                .getResultList();

        if (unsentMessages.isEmpty()) {
            System.out.println("No unsent messages to retry. Skipping...");
            return;
        }

        for (OutboxMessage msg : unsentMessages) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY,
                        msg.getPayload());

                outboxMessageRepository.markAsSent(msg.getPayload(), LocalDateTime.now());
                System.out.println("Resent message: " + msg.getPayload());

            } catch (Exception ex) {
                msg.setRetryCount(msg.getRetryCount() + 1);
                msg.setErrorMessage(ex.getMessage());
                entityManager.merge(msg);
                System.err.println("Retry failed for: " + msg.getPayload());
            }
        }
    }
}
