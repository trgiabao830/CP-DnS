package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Entity
@Data
@Table(name = "food_variants")
public class FoodVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;
    private String name;
    private Boolean isRequired;

    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "food_id")
    private Food food;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    @OrderBy("optionId ASC")
    private List<FoodVariantOption> options = new ArrayList<>() ;
}
