package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transId;

    private String bookingType;
    private Long bookingId;
    
    private BigDecimal amount;
    private String paymentGateway;
    private String gatewayTransId;
    private String status;
    private LocalDateTime createdAt;
}
