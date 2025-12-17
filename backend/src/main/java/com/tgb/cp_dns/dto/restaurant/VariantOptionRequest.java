package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

import com.tgb.cp_dns.enums.OptionStatus;

@Data
public class VariantOptionRequest {
    private Long optionId;

    @NotBlank(message = "Tên tùy chọn không được để trống")
    private String name;

    @NotNull(message = "Giá cộng thêm không được để trống")
    @Min(value = 0, message = "Giá không được âm")
    private BigDecimal priceAdjustment;

    private OptionStatus status; 
    private Long linkedFoodId;
}