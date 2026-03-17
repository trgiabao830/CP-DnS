package com.tgb.message_producer_service.consumer;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tgb.message_producer_service.model.OutboxMessage;
import com.tgb.message_producer_service.producer.MessageProducer;
import com.tgb.message_producer_service.repository.OutboxMessageRepository;

@Component
public class OutboxNotifierListener {

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private MessageProducer messageProducer;

    @RabbitListener(queues = "outbox.notify.queue")
    public void handleNotify(String message) {
        System.out.println("Received notify: " + message);

        List<OutboxMessage> pendingMessages = outboxMessageRepository.findAllBySentFalse();

        for (OutboxMessage msg : pendingMessages) {
            try {
                messageProducer.sendCancelOrderMessage(msg.getPayload());
            } catch (Exception e) {
                System.err.println("Send failed: " + msg.getPayload());
            }
        }
    }
}
