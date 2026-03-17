package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PaymentPreviewResponse {
    private boolean isDepositRequired;
    private BigDecimal depositAmount;
}
