package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Table(name = "foods")
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodId;
    private String name;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private String imageUrl;
    private Integer displayOrder;
    private String status;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private FoodCategory category;
    
    @OneToMany(mappedBy = "food")
    private List<FoodVariant> variants;
}
