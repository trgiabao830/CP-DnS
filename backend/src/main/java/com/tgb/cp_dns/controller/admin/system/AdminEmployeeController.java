package com.tgb.cp_dns.controller.admin.system;

import com.tgb.cp_dns.dto.employee.AdminResetPasswordRequest;
import com.tgb.cp_dns.dto.employee.CreateEmployeeRequest;
import com.tgb.cp_dns.dto.employee.EmployeeResponse;
import com.tgb.cp_dns.dto.employee.UpdateEmployeeRequest;
import com.tgb.cp_dns.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')") 
    public ResponseEntity<?> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        employeeService.createEmployee(request);
        return ResponseEntity.ok("Tạo nhân viên thành công");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<EmployeeResponse> getEmployeeDetail(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        employeeService.updateEmployee(id, request);
        return ResponseEntity.ok("Cập nhật nhân viên thành công");
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<?> resetEmployeePassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        
        employeeService.adminResetPassword(id, request.getNewPassword());
        return ResponseEntity.ok("Đã đặt lại mật khẩu cho nhân viên");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        employeeService.toggleEmployeeStatus(id);
        return ResponseEntity.ok("Thay đổi trạng thái nhân viên thành công");
    }
}
