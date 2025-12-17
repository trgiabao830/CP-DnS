package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.FoodStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateFoodStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    private FoodStatus status;
}
