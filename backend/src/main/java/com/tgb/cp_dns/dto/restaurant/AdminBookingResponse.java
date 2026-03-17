package com.tgb.cp_dns.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminBookingResponse {
    private Long bookingId;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    private LocalDateTime bookingTime;

    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    private LocalDateTime createdAt;

    private String status;
    private String tableNumber;
    private Integer numberOfGuests;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
}