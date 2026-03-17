package com.tgb.cp_dns.dto.homestay;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RoomTypeDetailResponse {
    private Long typeId;
    private String typeName;
    private String className;
    private String description;
    private BigDecimal basePrice;
    private Integer maxAdults;
    private Integer maxChildren;

    private List<String> images;
    private List<String> amenities;
    
    private Integer availableRoomsCount; 
}
