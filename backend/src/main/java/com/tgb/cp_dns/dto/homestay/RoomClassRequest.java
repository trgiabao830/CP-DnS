package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.HomestayCommonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomClassRequest {
    @NotBlank(message = "Tên hạng phòng không được để trống")
    private String name;

    @NotNull(message = "Trạng thái không được để trống")
    private HomestayCommonStatus status;
}