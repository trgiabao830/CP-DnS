package com.tgb.cp_dns.dto.employee;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EmployeeResponse {
    private Long empId;
    private String username;
    private String fullName;
    private String phone;
    private String jobTitle;
    private String status;
    private LocalDateTime createdAt;
    private List<PermissionDto> permissions;

    @Data
    @Builder
    public static class PermissionDto {
        private Long id;
        private String code;
        private String description;
    }
}