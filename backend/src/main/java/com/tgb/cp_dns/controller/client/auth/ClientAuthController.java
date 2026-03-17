package com.tgb.cp_dns.controller.client.auth;

import com.tgb.cp_dns.dto.auth.ForgotPasswordRequest;
import com.tgb.cp_dns.dto.auth.LoginRequest;
import com.tgb.cp_dns.dto.auth.RegisterRequest;
import com.tgb.cp_dns.dto.auth.ResetPasswordRequest;
import com.tgb.cp_dns.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginUser(request, httpRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.ok("Đăng ký tài khoản thành công");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Link đặt lại mật khẩu đã được gửi đến email của bạn.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Mật khẩu đã được thay đổi thành công.");
    }
}
