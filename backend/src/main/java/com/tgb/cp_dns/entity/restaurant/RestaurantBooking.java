package com.tgb.cp_dns.entity.restaurant;

import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;

import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.BookingType;
import com.tgb.cp_dns.enums.PaymentMethod;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "restaurant_bookings")
public class RestaurantBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @ManyToOne
    @JoinColumn(name = "homestay_booking_id")
    private HomestayBooking homestayBooking;

    @Enumerated(EnumType.STRING)
    private BookingType bookingType;

    private Boolean isChargedToRoom;

    @Column(columnDefinition = "json") 
    private String customerInfo;

    private LocalDateTime bookingTime;
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<RestaurantOrderDetail> orderDetails;
}
