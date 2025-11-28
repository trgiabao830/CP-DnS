package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Table(name = "restaurant_order_details")
public class RestaurantOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private RestaurantBooking booking;

    @ManyToOne
    @JoinColumn(name = "food_id")
    private Food food;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    private List<RestaurantOrderOption> selectedOptions;
}
