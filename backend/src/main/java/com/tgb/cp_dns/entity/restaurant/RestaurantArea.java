package com.tgb.cp_dns.entity.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.AreaStatus;

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

    @Enumerated(EnumType.STRING)
    private AreaStatus status;

    @JsonIgnore
    private Boolean isDeleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "area")
    private List<RestaurantTable> tables;
}
