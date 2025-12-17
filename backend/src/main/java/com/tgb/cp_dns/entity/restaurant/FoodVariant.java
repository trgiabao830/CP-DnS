package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
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

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "food_id")
    private Food food;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    private List<FoodVariantOption> options = new ArrayList<>() ;
}
