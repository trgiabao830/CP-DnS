package com.tgb.cp_dns.controller.admin.system;

import com.tgb.cp_dns.dto.employee.PermissionResponse;
import com.tgb.cp_dns.entity.auth.Permission;
import com.tgb.cp_dns.repository.auth.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAllPermissions(
            @RequestParam(required = false) String keyword) {

        List<Permission> permissions;

        if (keyword != null && !keyword.trim().isEmpty()) {
            permissions = permissionRepository.findByDescriptionContainingIgnoreCase(keyword.trim());
        } else {
            permissions = permissionRepository.findAll();
        }

        List<PermissionResponse> response = permissions.stream()
                .sorted(Comparator.comparing(Permission::getPermissionId))
                .map(p -> PermissionResponse.builder()
                        .permissionId(p.getPermissionId())
                        .code(p.getCode())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
