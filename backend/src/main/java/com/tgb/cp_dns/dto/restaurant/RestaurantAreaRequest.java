package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestaurantAreaRequest {
    @NotBlank(message = "Tên khu vực không được để trống")
    private String name;
}
