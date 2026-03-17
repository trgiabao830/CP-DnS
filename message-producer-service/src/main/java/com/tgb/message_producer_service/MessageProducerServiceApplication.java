package com.tgb.message_producer_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.tgb.message_producer_service.retry.RetryWorker;

@SpringBootApplication
public class MessageProducerServiceApplication {

    @Autowired
    private RetryWorker retryWorker;

    public static void main(String[] args) {
        SpringApplication.run(MessageProducerServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        retryWorker.retryUnsentMessagesOnce();
    }
}   