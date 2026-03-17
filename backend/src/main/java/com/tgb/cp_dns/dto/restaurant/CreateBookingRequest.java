package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.DepositType;
import com.tgb.cp_dns.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateBookingRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    @NotNull private Long tableId;
    @NotNull private LocalDate date;
    @NotNull private LocalTime time;
    @NotNull private Integer numberOfGuests;

    private Boolean isPreOrderFood;
    private List<OrderItemRequest> orderItems;

    private String couponCode;

    private DepositType depositType;

    private PaymentMethod paymentMethod;
}
