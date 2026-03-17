package com.tgb.cp_dns.controller.admin.auth;

import com.tgb.cp_dns.dto.auth.AuthResponse;
import com.tgb.cp_dns.dto.auth.LoginRequest;
import com.tgb.cp_dns.service.AuthService;
import com.tgb.cp_dns.service.SseNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final SseNotificationService sseService;

    @GetMapping("/sse/subscribe")
    public SseEmitter subscribeToEvents() {
        return sseService.subscribe();
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginEmployee(request, httpRequest));
    }

    @PostMapping("/refresh-permissions")
    public ResponseEntity<AuthResponse> refreshPermissions(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.refreshEmployeePermissions(request, response));
    }

}
