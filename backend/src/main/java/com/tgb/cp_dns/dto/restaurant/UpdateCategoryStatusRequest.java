package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCategoryStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private CategoryStatus status;
}