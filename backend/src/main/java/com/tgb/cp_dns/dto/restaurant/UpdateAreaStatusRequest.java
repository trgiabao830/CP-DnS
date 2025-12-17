package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.AreaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAreaStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private AreaStatus status;
}