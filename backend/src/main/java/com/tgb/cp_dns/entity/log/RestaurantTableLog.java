package com.tgb.cp_dns.entity.log;

import com.tgb.cp_dns.entity.restaurant.RestaurantTable;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "restaurant_table_logs")
public class RestaurantTableLog extends BaseLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Column(name = "action_type")
    private String actionType;
}
