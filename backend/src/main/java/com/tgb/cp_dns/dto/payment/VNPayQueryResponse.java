package com.tgb.cp_dns.dto.payment;

import lombok.Data;

@Data
public class VNPayQueryResponse {
    private String responseCode;
    private String transactionStatus;
    private String message;
    private String payDate;
}