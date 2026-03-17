package com.tgb.cp_dns.dto.homestay;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
public class AdminHomestayBookingDetailResponse {
    private Long bookingId;
    private String status;
    private LocalDateTime createdAt;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String roomNumber;
    private String roomClassName;
    private String roomName;
    private String roomImage;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfAdults;
    private Integer numberOfChildren;

    private String paymentMethod;
    @JsonFormat(pattern = "HH:mm:ss dd-MM-yyyy")
    private LocalDateTime paymentTime;
    private String vnpTxnRef;

    private BigDecimal pricePerNight;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;
}
