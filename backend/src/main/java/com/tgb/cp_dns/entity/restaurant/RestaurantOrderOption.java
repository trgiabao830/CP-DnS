package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "restaurant_order_options")
public class RestaurantOrderOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "detail_id")
    private RestaurantOrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private FoodVariantOption option;
}
