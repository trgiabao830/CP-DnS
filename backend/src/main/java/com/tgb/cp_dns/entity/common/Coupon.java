package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.enums.ServiceType;

@Entity
@Data
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;
    private String code;

    private Double discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private boolean requireAccount;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    @JsonIgnore
    private boolean isDeleted = false;
}
