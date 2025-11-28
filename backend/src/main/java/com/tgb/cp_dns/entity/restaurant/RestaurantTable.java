package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "restaurant_tables")
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableId;
    private String tableNumber;
    private Integer capacity;
    private String status;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private RestaurantArea area;
}
