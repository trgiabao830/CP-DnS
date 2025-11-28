package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "food_variants")
public class FoodVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;
    private String name;
    private Boolean isRequired;

    @ManyToOne
    @JoinColumn(name = "food_id")
    private Food food;

    @OneToMany(mappedBy = "variant")
    private List<FoodVariantOption> options;
}
