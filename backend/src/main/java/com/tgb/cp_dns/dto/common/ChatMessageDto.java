package com.tgb.cp_dns.dto.common;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessageDto {
    private Long msgId;
    private String guestSessionId;
    private String content;
    private String senderType;
    private String senderName;
    private String guestPhone;
    private String guestEmail;
    private LocalDateTime createdAt;
    private Long userId;
}
