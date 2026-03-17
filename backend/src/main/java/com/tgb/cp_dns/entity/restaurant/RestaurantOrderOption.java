package com.tgb.cp_dns.entity.restaurant;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "restaurant_order_options")
public class RestaurantOrderOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_id")
    private RestaurantOrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private FoodVariantOption option;

    private String variantNameSnapshot;

    private String optionNameSnapshot;

    private BigDecimal priceAdjustmentSnapshot;
}
