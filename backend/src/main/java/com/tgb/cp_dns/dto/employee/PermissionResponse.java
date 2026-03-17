package com.tgb.cp_dns.dto.employee;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {
    private Long permissionId;
    private String code;
    private String description;
}
