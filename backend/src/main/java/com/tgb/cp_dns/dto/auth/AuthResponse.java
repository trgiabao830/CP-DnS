package com.tgb.cp_dns.dto.auth;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String name;
    private String role;
    private Set<String> permissions;
}