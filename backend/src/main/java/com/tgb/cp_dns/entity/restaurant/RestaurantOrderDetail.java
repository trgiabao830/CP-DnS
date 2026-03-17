package com.tgb.cp_dns.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Entity
@Data
@Table(name = "restaurant_order_details")
public class RestaurantOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private RestaurantBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    private String foodNameSnapshot;
    private String foodImageSnapshot;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    @Column(length = 200) 
    private String note;

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL)
    @BatchSize(size = 10)
    private List<RestaurantOrderOption> selectedOptions = new ArrayList<>();
}
