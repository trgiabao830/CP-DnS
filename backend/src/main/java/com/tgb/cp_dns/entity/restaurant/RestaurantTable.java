package com.tgb.cp_dns.entity.restaurant;

import com.tgb.cp_dns.enums.TableStatus;

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

    @Enumerated(EnumType.STRING)
    private TableStatus status;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private RestaurantArea area;
}
