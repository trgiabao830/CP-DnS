package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RestaurantOverviewResponse {
    private List<AreaSnapshotDto> areas;
}