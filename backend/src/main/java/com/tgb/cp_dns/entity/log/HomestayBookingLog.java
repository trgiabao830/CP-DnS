package com.tgb.cp_dns.entity.log;

import com.tgb.cp_dns.entity.homestay.HomestayBooking;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "homestay_booking_logs")
public class HomestayBookingLog extends BaseLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private HomestayBooking booking;

    @Column(name = "action_type")
    private String actionType;
}