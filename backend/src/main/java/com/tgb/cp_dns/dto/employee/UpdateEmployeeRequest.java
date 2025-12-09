package com.tgb.cp_dns.dto.employee;

import lombok.Data;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Data
public class UpdateEmployeeRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, message = "Username phải từ 4 ký tự")
    private String username;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 11, message = "Số điện thoại phải từ 10-11 số")
    private String phone;

    @NotBlank(message = "Chức vụ không được để trống")
    private String jobTitle;

    @NotEmpty(message = "Phải cấp ít nhất 1 quyền hạn")
    private Set<Long> permissionIds;
}
