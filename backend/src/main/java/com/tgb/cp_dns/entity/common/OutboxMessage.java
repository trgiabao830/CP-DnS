package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
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

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
