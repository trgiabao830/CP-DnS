package com.tgb.cp_dns.dto.restaurant;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataResponse {
    private String label;
    private BigDecimal value;
}