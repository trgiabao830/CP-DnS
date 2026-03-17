package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.auth.ChangePasswordRequest;
import com.tgb.cp_dns.dto.employee.CreateEmployeeRequest;
import com.tgb.cp_dns.dto.employee.EmployeeResponse;
import com.tgb.cp_dns.dto.employee.UpdateEmployeeRequest;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.Permission;
import com.tgb.cp_dns.enums.EmployeeStatus;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.PermissionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SseNotificationService sseService;

    @Transactional
    public Employee createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByUsernameAndIsDeletedFalse(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại");
        }

        if (employeeRepository.existsByPhoneAndIsDeletedFalse(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại nhân viên đã tồn tại");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và mật khẩu xác nhận không khớp.");
        }

        Set<Permission> permissions = permissionRepository.findAllByPermissionIdIn(request.getPermissionIds());
        if (permissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách quyền hạn không hợp lệ");
        }

        Employee emp = new Employee();
        emp.setUsername(request.getUsername());
        emp.setFullName(request.getFullName());
        emp.setPhone(request.getPhone());
        emp.setJobTitle(request.getJobTitle());
        emp.setPassword(passwordEncoder.encode(request.getPassword()));
        emp.setStatus(EmployeeStatus.ACTIVE);
        emp.setCreatedAt(LocalDateTime.now());
        emp.setPermissions(permissions);

        Employee savedEmp = employeeRepository.save(emp);
        sseService.sendNotification("EMPLOYEE_UPDATE", savedEmp.getEmpId());
        return savedEmp;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(String keyword, String statusStr, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "createdAt"));

        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        EmployeeStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = EmployeeStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        Page<Employee> employeePage = employeeRepository.searchEmployees(finalKeyword, finalStatus, sortedPageable);

        return employeePage.map(emp -> EmployeeResponse.builder()
                .empId(emp.getEmpId())
                .username(emp.getUsername())
                .fullName(emp.getFullName())
                .phone(emp.getPhone())
                .jobTitle(emp.getJobTitle())
                .status(emp.getStatus().name())
                .createdAt(emp.getCreatedAt())
                .permissions(emp.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getPermissionId))
                        .map(p -> EmployeeResponse.PermissionDto.builder()
                                .id(p.getPermissionId())
                                .code(p.getCode())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .build());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeDetail(Long empId) {
        Employee emp = employeeRepository.findByEmpIdAndIsDeletedFalse(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        return EmployeeResponse.builder()
                .empId(emp.getEmpId())
                .username(emp.getUsername())
                .fullName(emp.getFullName())
                .phone(emp.getPhone())
                .jobTitle(emp.getJobTitle())
                .status(emp.getStatus().name())
                .createdAt(emp.getCreatedAt())
                .permissions(emp.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getPermissionId))
                        .map(p -> EmployeeResponse.PermissionDto.builder()
                                .id(p.getPermissionId())
                                .code(p.getCode())
                                .description(p.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getMyProfile(Long empId) {
        return getEmployeeDetail(empId);
    }

    @Transactional
    public void updateEmployee(Long empId, UpdateEmployeeRequest request) {
        Employee emp = employeeRepository.findByEmpIdAndIsDeletedFalse(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (employeeRepository.existsByUsernameAndIsDeletedFalse(request.getUsername()) &&
                    !request.getUsername().equals(emp.getUsername())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại");
            }
            emp.setUsername(request.getUsername());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            emp.setFullName(request.getFullName());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (employeeRepository.existsByPhoneAndIsDeletedFalse(request.getPhone()) &&
                    !request.getPhone().equals(emp.getPhone())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại nhân viên đã tồn tại");
            }
            emp.setPhone(request.getPhone());
        }

        if (request.getJobTitle() != null && !request.getJobTitle().isBlank()) {
            emp.setJobTitle(request.getJobTitle());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> newPermissions = permissionRepository.findAllByPermissionIdIn(request.getPermissionIds());
            emp.setPermissions(newPermissions);
        }

        employeeRepository.save(emp);
        sseService.sendNotification("EMPLOYEE_UPDATE", empId);
    }

    @Transactional
    public void toggleEmployeeStatus(Long empId) {
        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        if (emp.getStatus() == EmployeeStatus.ACTIVE) {
            emp.setStatus(EmployeeStatus.INACTIVE);
        } else {
            emp.setStatus(EmployeeStatus.ACTIVE);
        }
        employeeRepository.save(emp);
        sseService.sendNotification("EMPLOYEE_UPDATE", empId);
    }

    @Transactional
    public void changePassword(Long empId, ChangePasswordRequest request) {
        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        if (!passwordEncoder.matches(request.getOldPassword(), emp.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        emp.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepository.save(emp);
    }

    @Transactional
    public void adminResetPassword(Long empId, String newPassword) {
        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        emp.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(emp);
    }

    @Transactional
    public void deleteEmployee(Long empId) {
        Employee emp = employeeRepository.findByEmpIdAndIsDeletedFalse(empId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên không tồn tại"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (emp.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không thể tự xóa tài khoản của chính mình.");
        }

        emp.setDeleted(true);
        emp.setStatus(EmployeeStatus.INACTIVE);

        employeeRepository.save(emp);
        sseService.sendNotification("EMPLOYEE_UPDATE", empId);
    }
}
