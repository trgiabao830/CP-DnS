package com.tgb.message_producer_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean sent = false;
    private boolean processed = false;
    private int retryCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
