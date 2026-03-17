package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.entity.auth.User;

@Entity
@Data
@Table(name = "homestay_bookings")
public class HomestayBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private String roomClassNameSnapshot;
    private String roomNameSnapshot;
    private BigDecimal pricePerNightSnapshot;
    private String roomImageSnapshot;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private Integer numberOfAdults;
    private Integer numberOfChildren;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal subTotal;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentTime;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String vnpTxnRef;
    private String accessToken;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String appliedCouponCode;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingChildAge> childAges;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.accessToken == null) {
            this.accessToken = UUID.randomUUID().toString();
        }
    }
}
