package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingCreationResponse {
        private String bookingCode;
        private String message;
        private String paymentUrl;
    }
