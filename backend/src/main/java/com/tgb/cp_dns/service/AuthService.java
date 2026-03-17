package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.auth.AuthResponse;
import com.tgb.cp_dns.dto.auth.ForgotPasswordRequest;
import com.tgb.cp_dns.dto.auth.LoginRequest;
import com.tgb.cp_dns.dto.auth.RegisterRequest;
import com.tgb.cp_dns.dto.auth.ResetPasswordRequest;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.PasswordResetToken;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.enums.EmployeeStatus;
import com.tgb.cp_dns.enums.UserStatus;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.PasswordResetTokenRepository;
import com.tgb.cp_dns.repository.auth.UserRepository;
import com.tgb.cp_dns.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private final EmployeeRepository employeeRepository;
    private final SecurityContextRepository securityContextRepository;

    private static final int TIMEOUT_USER = 2 * 60 * 60;
    private static final int TIMEOUT_STAFF = 4 * 60 * 60;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request, HttpServletRequest httpRequest) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if (securityUser.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

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
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        if (securityUser.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Thông tin tài khoản hoặc mật khẩu không hợp lệ");
        }

        Employee emp = securityUser.getEmployee();

        createSession(authentication, httpRequest);

        return AuthResponse.builder()
                .id(emp.getEmpId())
                .name(emp.getFullName())
                .role(emp.getJobTitle())
                .permissions(emp.getPermissions().stream()
                        .map(p -> AuthResponse.PermissionDto.builder()
                                .code(p.getCode())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshEmployeePermissions(HttpServletRequest request, HttpServletResponse response) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth == null || !(currentAuth.getPrincipal() instanceof SecurityUser)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }

        SecurityUser currentUser = (SecurityUser) currentAuth.getPrincipal();
        Employee currentEmpData = currentUser.getEmployee();

        if (currentEmpData == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API này chỉ dành cho nhân viên");
        }

        Employee freshEmp = employeeRepository.findByEmpIdAndIsDeletedFalse(currentEmpData.getEmpId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));

        if (freshEmp.getStatus() != EmployeeStatus.ACTIVE) {
            SecurityContextHolder.clearContext();
            securityContextRepository.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        SecurityUser newSecurityUser = new SecurityUser(freshEmp);

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                newSecurityUser,
                currentAuth.getCredentials(),
                newSecurityUser.getAuthorities());

        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(newAuth);
        SecurityContextHolder.setContext(newContext);

        securityContextRepository.saveContext(newContext, request, response);

        return AuthResponse.builder()
                .id(freshEmp.getEmpId())
                .name(freshEmp.getFullName())
                .role(freshEmp.getJobTitle())
                .permissions(freshEmp.getPermissions().stream()
                        .map(p -> AuthResponse.PermissionDto.builder()
                                .code(p.getCode())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        String fullName = request.getFullName().trim();
        String rawPassword = request.getPassword().trim();
        String confirmPassword = request.getConfirmPassword().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng");
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng");
        }

        if (!rawPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và mật khẩu xác nhận không khớp.");
        }

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setDob(request.getDob());
        newUser.setGender(request.getGender());
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setStatus(UserStatus.ACTIVE);

        userRepository.save(newUser);
    }

    private void createSession(Authentication authentication, HttpServletRequest request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isStaff) {
            session.setMaxInactiveInterval(TIMEOUT_STAFF);
        } else {
            session.setMaxInactiveInterval(TIMEOUT_USER);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        PasswordResetToken resetToken = tokenRepository.findByUser(user)
                .orElse(new PasswordResetToken());

        String newTokenString = UUID.randomUUID().toString();

        resetToken.setUser(user);
        resetToken.setToken(newTokenString);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        tokenRepository.save(resetToken);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + newTokenString;
        emailService.sendResetPasswordEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token không hợp lệ hoặc đã hết hạn"));

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