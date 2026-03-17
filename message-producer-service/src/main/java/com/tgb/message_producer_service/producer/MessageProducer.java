package com.tgb.message_producer_service.producer;

import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgb.message_producer_service.config.RabbitMQConfig;
import com.tgb.message_producer_service.repository.OutboxMessageRepository;
import com.tgb.message_producer_service.retry.RetryWorker;

@Component
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private RetryWorker retryWorker;

    public void sendCancelOrderMessage(String vnpTxnRef) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                vnpTxnRef
            );

            outboxMessageRepository.markAsSent(vnpTxnRef, LocalDateTime.now());
            System.out.println("Message sent: " + vnpTxnRef);

        } catch (Exception ex) {
            System.err.println("Failed to send message: " + ex.getMessage());
            retryWorker.retryUnsentMessagesOnce();
        }
    }
}
