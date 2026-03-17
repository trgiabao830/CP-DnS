package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.FoodStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateFoodStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private FoodStatus status;
}
