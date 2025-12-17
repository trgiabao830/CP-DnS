package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.CategoryStatus;

@Entity
@Data
@Table(name = "food_categories")
public class FoodCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    private String name;
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    private CategoryStatus status;

    @JsonIgnore
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<Food> foods;
}
