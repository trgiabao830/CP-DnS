package com.tgb.cp_dns.dto.common;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SupportMessageResponse {
    private Long msgId;
    private String message;
    private String senderType;
    private Long senderId;
    private String senderName;
    private LocalDateTime createdAt;
    
}
