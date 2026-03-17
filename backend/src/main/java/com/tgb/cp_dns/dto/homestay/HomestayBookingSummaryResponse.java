package com.tgb.cp_dns.dto.homestay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayBookingSummaryResponse {
    private Long bookingId;
    private String accessToken;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private String status;
    
    private String roomName;
    private String roomImage;
    
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
}
