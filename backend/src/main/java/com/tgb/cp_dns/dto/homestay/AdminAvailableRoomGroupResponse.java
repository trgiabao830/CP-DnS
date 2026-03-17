package com.tgb.cp_dns.dto.homestay;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminAvailableRoomGroupResponse {
    private Long classId;
    private String className;
    private List<RoomTypeGroup> roomTypes;

    @Data
    @Builder
    public static class RoomTypeGroup {
        private Long typeId;
        private String typeName;
        private BigDecimal pricePerNight;
        private Integer maxAdults;
        private Integer maxChildren;
        private List<RoomDetail> rooms;
    }

    @Data
    @Builder
    public static class RoomDetail {
        private Long roomId;
        private String roomNumber;
        private boolean isCurrentRoom;
    }
}