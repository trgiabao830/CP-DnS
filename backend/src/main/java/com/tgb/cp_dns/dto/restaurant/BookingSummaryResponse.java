package com.tgb.cp_dns.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingSummaryResponse {
    private Long bookingId;
    private String accessToken;
    
    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    private LocalDateTime bookingTime;
    
    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    private LocalDateTime createdAt;
    
    private String status;
    private Integer numberOfGuests;
    
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
}
