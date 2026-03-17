package com.tgb.cp_dns.dto.restaurant;

import lombok.Data;
import java.util.List;

@Data
public class OrderItemRequest {
    private Long foodId;
    private Integer quantity;
    private List<Long> optionIds;
    private String note;
}