package com.tgb.cp_dns.dto.common;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponse {
    private Long couponId;
    private String code;
    
    private Double discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    
    private LocalDateTime validUntil;
    private boolean isRequireAccount;
}