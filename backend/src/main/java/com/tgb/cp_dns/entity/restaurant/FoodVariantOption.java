package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

import com.tgb.cp_dns.enums.OptionStatus;

@Entity
@Data
@Table(name = "food_variant_options")
public class FoodVariantOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;
    private String name;
    private BigDecimal priceAdjustment;

    @Enumerated(EnumType.STRING)
    private OptionStatus status = OptionStatus.AVAILABLE;

    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private FoodVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_food_id")
    private Food linkedFood;
}
