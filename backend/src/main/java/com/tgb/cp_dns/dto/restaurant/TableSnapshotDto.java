package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.TableStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TableSnapshotDto {
    private Long tableId;
    private String tableNumber;
    private Integer capacity;
    private TableStatus currentStatus;
    
    private ShortBookingDto currentBooking; 
    
    private List<ShortBookingDto> todayBookings; 
}