package com.tgb.cp_dns.dto.restaurant;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingDetailResponse {
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") 
    private LocalDateTime bookingTime;      
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Integer cancellationNoticeHours;
    
    private Integer numberOfGuests;
    private String tableNumber;
    
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;
    
    private String paymentMethod;
    private String status;
    private String bookingType;
    
    private List<DetailItemDto> orderItems;

    @Data
    @Builder
    public static class DetailItemDto {
        private String foodName;
        private String foodImage;
        private int quantity;
        private String note;
        
        private BigDecimal unitPrice; 
        private BigDecimal totalPrice;
        
        private List<String> options;
    }
}