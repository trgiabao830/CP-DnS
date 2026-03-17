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
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

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

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private LocalDateTime bookingTime;
    private LocalDateTime endTime;
    private Integer numberOfGuests;

    private BigDecimal subTotal;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private LocalDateTime paymentTime;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private String accessToken;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    private String appliedCouponCode;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<RestaurantOrderDetail> orderDetails;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String vnpTxnRef;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.accessToken == null) {
            this.accessToken = UUID.randomUUID().toString();
        }
    }
}
