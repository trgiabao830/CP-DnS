package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.enums.DepositType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHomestayBookingRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private Long roomTypeId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfAdults;
    private Integer numberOfChildren;

    private PaymentMethod paymentMethod;
    private DepositType depositType;
    private String couponCode;
    
}