package com.tgb.cp_dns.dto.user;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String status;
}
