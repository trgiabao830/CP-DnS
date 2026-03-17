package com.tgb.cp_dns.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminBookingDetailResponse {
    private Long bookingId;
    private String status;
    private String bookingType;
    @JsonFormat(pattern = "HH:mm:ss dd-MM-yyyy")
    private LocalDateTime createdAt;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String tableNumber;
    private Integer numberOfGuests;
    @JsonFormat(pattern = "HH:mm dd-MM-yyyy")
    private LocalDateTime bookingTime;

    private String paymentMethod;
    @JsonFormat(pattern = "HH:mm:ss dd-MM-yyyy")
    private LocalDateTime paymentTime;
    private String vnpTxnRef;

    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;

    private List<AdminOrderedItemDto> orderItems;

    @Data
    @Builder
    public static class AdminOrderedItemDto {
        private Long detailId;
        private Long foodId;
        private String foodName;
        private String foodImage;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String note;
        private List<String> options;
    }
}
