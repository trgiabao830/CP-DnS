package com.tgb.cp_dns.dto.restaurant;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalkInBookingRequest {
    @NotNull(message = "Phải chọn bàn")
    private Long tableId;

    private String customerName;
    private String customerPhone;

    private Integer numberOfGuests;

    private List<OrderItemRequest> orderItems;
}
