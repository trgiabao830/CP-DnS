package com.tgb.cp_dns.controller.admin.system;

import com.tgb.cp_dns.dto.auth.ChangePasswordRequest;
import com.tgb.cp_dns.dto.employee.EmployeeResponse;
import com.tgb.cp_dns.security.SecurityUser;
import com.tgb.cp_dns.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<EmployeeResponse> getMyProfile(@AuthenticationPrincipal SecurityUser currentUser) {
        return ResponseEntity.ok(employeeService.getMyProfile(currentUser.getEmployee().getEmpId()));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long empId = securityUser.getEmployee().getEmpId();

        employeeService.changePassword(empId, request);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
