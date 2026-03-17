package com.tgb.cp_dns.dto.common;

import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {
    @NotBlank(message = "Mã giảm giá không được để trống")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã chỉ được chứa chữ hoa và số, không dấu cách")
    private String code;

    @Min(value = 0, message = "Phần trăm giảm giá không hợp lệ")
    @Max(value = 100, message = "Phần trăm giảm giá tối đa 100")
    private Double discountPercent;

    @Min(value = 0, message = "Số tiền giảm giá không hợp lệ")
    private BigDecimal discountAmount;

    @Min(value = 0, message = "Giảm tối đa không hợp lệ")
    private BigDecimal maxDiscountAmount;

    @Min(value = 0, message = "Giá trị đơn hàng tối thiểu không hợp lệ")
    private BigDecimal minOrderValue;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Loại dịch vụ không được để trống")
    private ServiceType serviceType;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime validUntil;

    private boolean requireAccount;

    @NotNull(message = "Trạng thái không được để trống")
    private CouponStatus status;
}