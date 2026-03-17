package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableTableResponse {
    private Long areaId;
    private String areaName;
    private Integer capacity;
    private Long suggestedTableId;
    private String tableName;
    private Integer remainingTables;
}
