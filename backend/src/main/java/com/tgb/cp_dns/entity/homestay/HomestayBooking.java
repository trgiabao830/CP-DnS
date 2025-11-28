package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.tgb.cp_dns.enums.BookingStatus;
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

    @Column(columnDefinition = "json")
    private String customerInfo;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private String paymentOption;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingChildAge> childAges;
}
