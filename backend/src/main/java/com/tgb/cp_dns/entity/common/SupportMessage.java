package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "support_messages")
public class SupportMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long msgId;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private SupportSession session;

    private String senderType;
    private Long senderId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}
