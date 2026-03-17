package com.tgb.cp_dns.config;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPayConfig {
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

        @Value("${vnpay.return-url-homestay}")
    private String returnUrlHomestay;
    
    @Value("${vnpay.query-url}")
    private String queryUrl; 
}
