package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "booking_child_ages")
public class BookingChildAge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private HomestayBooking booking;
    
    private Integer age;
}
