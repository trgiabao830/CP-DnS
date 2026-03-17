package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
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
public class HomestayBookingDetailResponse {
    private Long bookingId;
    private String accessToken;
    private BookingStatus status;
    private LocalDateTime createdAt;

    private Integer cancellationNoticeHours;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String roomNumber;
    private String roomClass;

    private String roomName;
    private String roomImage;
    private BigDecimal pricePerNight;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfAdults;
    private Integer numberOfChildren;

    private PaymentMethod paymentMethod;
    private LocalDateTime paymentTime;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private String appliedCouponCode;
    private String vnpTxnRef;
}
