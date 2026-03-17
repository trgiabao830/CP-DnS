package com.tgb.cp_dns.dto.homestay;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HomestayOverviewResponse {
    private List<ShortBookingInfo> arrivingToday;
    private List<ShortBookingInfo> departingToday;
    
    private List<RoomClassGroup> roomMap;

    @Data
    @Builder
    public static class ShortBookingInfo {
        private Long bookingId;
        private String customerName;
        private String roomNumber;
        private String status;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    public static class RoomClassGroup {
        private Long classId;
        private String className;
        private List<RoomTypeGroup> roomTypes;
    }

    @Data
    @Builder
    public static class RoomTypeGroup {
        private Long typeId;
        private String typeName;
        private BigDecimal pricePerNight;
        private List<RoomSnapshot> rooms;
    }

    @Data
    @Builder
    public static class RoomSnapshot {
        private Long roomId;
        private String roomNumber;
        private String status;
        private Long currentBookingId;
        private String currentCustomerName;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
    }
}