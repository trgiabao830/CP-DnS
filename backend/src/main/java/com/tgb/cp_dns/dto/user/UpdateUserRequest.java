package com.tgb.cp_dns.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tgb.cp_dns.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 11, message = "Số điện thoại phải từ 10-11 số")
    private String phone;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
}
