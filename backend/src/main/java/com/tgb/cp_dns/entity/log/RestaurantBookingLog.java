package com.tgb.cp_dns.entity.log;

import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "restaurant_booking_logs")
public class RestaurantBookingLog extends BaseLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private RestaurantBooking booking;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;
}