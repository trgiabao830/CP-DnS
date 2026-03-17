package com.tgb.cp_dns.dto.common;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SupportSessionResponse {
    private Long sessionId;
    private String guestSessionId;
    
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    
    private String status;
    private LocalDateTime createdAt;

    private Long userId; 
}
