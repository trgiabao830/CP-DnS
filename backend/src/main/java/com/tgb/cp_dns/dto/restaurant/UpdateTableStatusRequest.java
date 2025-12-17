package com.tgb.cp_dns.dto.restaurant;

import com.tgb.cp_dns.enums.TableStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTableStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private TableStatus status;
}
