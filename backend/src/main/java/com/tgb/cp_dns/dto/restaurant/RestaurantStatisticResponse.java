package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class RestaurantStatisticResponse {
    private MetricDetail today;
    private MetricDetail thisWeek;
    private MetricDetail thisMonth;
    private MetricDetail total;

    @Data
    @Builder
    public static class MetricDetail {
        private BigDecimal revenue;
        private Long completedOrders;
    }
}