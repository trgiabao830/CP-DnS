package com.tgb.cp_dns.controller.client.account;

import com.tgb.cp_dns.dto.auth.ChangePasswordRequest;
import com.tgb.cp_dns.dto.user.UpdateUserRequest;
import com.tgb.cp_dns.dto.user.UserResponse;
import com.tgb.cp_dns.security.SecurityUser;
import com.tgb.cp_dns.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(userService.getUserProfile(currentUser.getUser().getUserId()));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getUser().getUserId(), request));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long userId = securityUser.getUser().getUserId();

        userService.changePassword(userId, request);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
