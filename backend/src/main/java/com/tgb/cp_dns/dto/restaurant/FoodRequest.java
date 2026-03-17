package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.tgb.cp_dns.enums.FoodStatus;

@Data
public class FoodRequest {
    @NotBlank(message = "Tên món ăn không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 0)
    private BigDecimal basePrice;

    private BigDecimal discountPrice;

    @NotNull(message = "Trạng thái không được để trống")
    private FoodStatus status;
    private String imageUrl;
    private Integer displayOrder;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    private List<VariantRequest> variants = new ArrayList<>();
}
