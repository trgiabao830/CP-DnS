package com.tgb.cp_dns.dto.common;

import lombok.Data;

@Data
public class SessionActionRequest {
    private Long sessionId;
    private String action;
}
