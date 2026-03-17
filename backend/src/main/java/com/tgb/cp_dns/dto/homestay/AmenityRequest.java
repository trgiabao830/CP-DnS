package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.HomestayCommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AmenityRequest {
    @NotBlank(message = "Tên tiện ích không được để trống")
    private String name;

    @NotNull(message = "Trạng thái không được để trống")
    private HomestayCommonStatus status;
}