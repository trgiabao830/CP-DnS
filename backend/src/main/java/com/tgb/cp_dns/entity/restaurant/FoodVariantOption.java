package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "food_variant_options")
public class FoodVariantOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;
    private String name;
    private BigDecimal priceAdjustment;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private FoodVariant variant;
}
