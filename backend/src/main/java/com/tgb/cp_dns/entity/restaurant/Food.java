package com.tgb.cp_dns.entity.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.FoodStatus;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Entity
@Data
@Table(name = "foods")
@BatchSize(size = 20)
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodId;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private String imageUrl;
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    private FoodStatus status;

    @JsonIgnore
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private FoodCategory category;
    
    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL)
    @OrderBy("variantId ASC")
    @BatchSize(size = 20)
    private List<FoodVariant> variants = new ArrayList<>();
}
