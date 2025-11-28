package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "restaurant_areas")
public class RestaurantArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long areaId;
    private String name;
    private Boolean status;

    @OneToMany(mappedBy = "area")
    private List<RestaurantTable> tables;
}
