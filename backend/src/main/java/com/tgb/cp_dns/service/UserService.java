package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.auth.ChangePasswordRequest;
import com.tgb.cp_dns.dto.user.AdminCreateUserRequest;
import com.tgb.cp_dns.dto.user.UpdateUserRequest;
import com.tgb.cp_dns.dto.user.UserResponse;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.enums.UserStatus;
import com.tgb.cp_dns.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SseNotificationService sseService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, String statusStr, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "userId"));

        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        UserStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = UserStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        Page<User> userPage = userRepository.searchUsers(finalKeyword, finalStatus, sortedPageable);
        return userPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserDetail(Long id) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        return getUserDetail(userId);
    }

    @Transactional
    public void createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng");
        }

        if (userRepository.existsByPhoneAndIsDeletedFalse(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và mật khẩu xác nhận không khớp.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGender(request.getGender());
        user.setDob(request.getDob());
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);

        User savedUser = userRepository.save(user);
        sseService.sendNotification("USER_UPDATE", savedUser.getUserId());
    }

    @Transactional
    public void updateUserByAdmin(Long userId, UpdateUserRequest request) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        String newPhone = request.getPhone() != null ? request.getPhone().trim() : null;
        String newEmail = request.getEmail() != null ? request.getEmail().trim() : null;

        if (newPhone != null && !newPhone.isBlank()) {
            if (!newPhone.equals(user.getPhone()) && userRepository.existsByPhoneAndIsDeletedFalse(newPhone)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng");
            }
            user.setPhone(newPhone);
        }

        if (newEmail != null && !newEmail.isBlank()) {
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmailAndIsDeletedFalse(newEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng");
            }
            user.setEmail(newEmail);
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userRepository.save(user);
        sseService.sendNotification("USER_UPDATE", userId);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        sseService.sendNotification("USER_UPDATE", id);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        user.setDeleted(true);

        userRepository.save(user);
        sseService.sendNotification("USER_UPDATE", userId);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        updateUserByAdmin(userId, request);
        return getUserDetail(userId);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDob())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .status(user.getStatus().name())
                .build();
    }
}