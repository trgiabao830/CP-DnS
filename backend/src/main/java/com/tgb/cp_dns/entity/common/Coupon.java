package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;
    private String code;
    private BigDecimal discountAmount;
    private Integer quantity;
    private String serviceType;
}