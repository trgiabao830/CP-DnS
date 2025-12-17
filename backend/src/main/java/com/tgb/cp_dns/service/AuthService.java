package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.auth.AuthResponse;
import com.tgb.cp_dns.dto.auth.ForgotPasswordRequest;
import com.tgb.cp_dns.dto.auth.LoginRequest;
import com.tgb.cp_dns.dto.auth.RegisterRequest;
import com.tgb.cp_dns.dto.auth.ResetPasswordRequest;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.PasswordResetToken;
import com.tgb.cp_dns.entity.auth.Permission;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.enums.UserStatus;
import com.tgb.cp_dns.repository.auth.PasswordResetTokenRepository;
import com.tgb.cp_dns.repository.auth.UserRepository;
import com.tgb.cp_dns.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request, HttpServletRequest httpRequest) {
        
        Authentication authentication;
        
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        User user = securityUser.getUser();

        createSession(authentication, httpRequest);

        return AuthResponse.builder()
            .id(user.getUserId())
            .name(user.getFullName())
            .role("USER")
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse loginEmployee(LoginRequest request, HttpServletRequest httpRequest) {
        
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Employee emp = securityUser.getEmployee();

        createSession(authentication, httpRequest);

        return AuthResponse.builder()
                .id(emp.getEmpId())
                .name(emp.getFullName())
                .role("EMPLOYEE")
                .permissions(emp.getPermissions().stream()
                    .map(Permission::getCode)
                    .collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng");
        }
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và mật khẩu xác nhận không khớp."); 
        }

        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setDob(request.getDob());
        newUser.setGender(request.getGender());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setStatus(UserStatus.ACTIVE);

        userRepository.save(newUser);
    }

    private void createSession(Authentication authentication, HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        Optional<PasswordResetToken> oldToken = tokenRepository.findByUser(user);
        oldToken.ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        
        emailService.sendResetPasswordEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn, vui lòng yêu cầu lại");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}