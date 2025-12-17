package com.tgb.cp_dns.entity.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.FoodStatus;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "foods")
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
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private FoodCategory category;
    
    @OneToMany(mappedBy = "food", cascade = CascadeType.ALL)
    private List<FoodVariant> variants = new ArrayList<>();
}
