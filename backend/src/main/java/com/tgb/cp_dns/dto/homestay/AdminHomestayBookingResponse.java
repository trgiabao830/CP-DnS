package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminHomestayBookingResponse {
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private String roomNumber;
    private String roomClassName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
