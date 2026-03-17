package com.tgb.cp_dns.dto.homestay;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class HomestayStatisticResponse {
    private MetricDetail today;
    private MetricDetail thisWeek;
    private MetricDetail thisMonth;
    private MetricDetail total;

    @Data
    @Builder
    public static class MetricDetail {
        private BigDecimal revenue;
        private long completedOrders;
    }
}