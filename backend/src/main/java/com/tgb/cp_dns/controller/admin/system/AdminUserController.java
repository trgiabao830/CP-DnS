package com.tgb.cp_dns.controller.admin.system;

import com.tgb.cp_dns.dto.user.AdminCreateUserRequest;
import com.tgb.cp_dns.dto.user.UserResponse;
import com.tgb.cp_dns.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')") 
    public ResponseEntity<Page<UserResponse>> getUsers(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<?> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        userService.createUserByAdmin(request);
        return ResponseEntity.ok("Tạo người dùng thành công");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok("Thay đổi trạng thái người dùng thành công");
    }
}