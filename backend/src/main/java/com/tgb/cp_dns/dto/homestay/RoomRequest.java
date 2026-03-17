package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequest {
    @NotBlank(message = "Số phòng/Tên phòng không được để trống")
    private String roomNumber;

    @NotNull(message = "Vui lòng chọn loại phòng (Type ID)")
    private Long typeId;

    @NotNull(message = "Trạng thái phòng không được để trống")
    private RoomStatus status;
}