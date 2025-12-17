package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RestaurantTableRequest {
    @NotBlank(message = "Số bàn/Tên bàn không được để trống")
    private String tableNumber;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    private Integer capacity;

    @NotNull(message = "Vui lòng chọn khu vực")
    private Long areaId;
}
