package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShortBookingDto {
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private LocalDateTime bookingTime; 
    private LocalDateTime endTime;     
    private BookingStatus status;      
    private Integer numberOfGuests;
}