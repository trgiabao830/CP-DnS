package com.tgb.cp_dns.config;

import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.Permission;
import com.tgb.cp_dns.enums.EmployeeStatus;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.admin.name}")
    private String adminName;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createPermissionIfNotExists("EMPLOYEE_VIEW", "Xem danh sách nhân viên", "EMPLOYEE");
        createPermissionIfNotExists("EMPLOYEE_CREATE", "Tạo nhân viên mới", "EMPLOYEE");
        createPermissionIfNotExists("EMPLOYEE_UPDATE", "Cập nhật nhân viên", "EMPLOYEE");
        createPermissionIfNotExists("EMPLOYEE_DELETE", "Xóa/Khóa nhân viên", "EMPLOYEE");

        createPermissionIfNotExists("USER_VIEW", "Xem danh sách khách hàng", "USER");
        createPermissionIfNotExists("USER_CREATE", "Tạo khách hàng mới", "USER");
        createPermissionIfNotExists("USER_LOCK", "Khóa tài khoản khách hàng", "USER");

        createPermissionIfNotExists("RESTAURANT_VIEW", "Xem danh sách bàn/món ăn", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_CREATE", "Tạo bàn/món ăn mới", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_UPDATE", "Cập nhật bàn/món ăn", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_DELETE", "Xóa bàn/món ăn", "RESTAURANT");
        
        if (!employeeRepository.existsByUsername(adminUsername)) {
            Employee admin = new Employee();
            
            admin.setUsername(adminUsername);
            admin.setFullName(adminName);
            admin.setPhone(adminPhone);
            admin.setJobTitle("Quản trị hệ thống");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            
            admin.setStatus(EmployeeStatus.ACTIVE);
            admin.setCreatedAt(LocalDateTime.now());

            List<Permission> allPermissions = permissionRepository.findAll();
            admin.setPermissions(new HashSet<>(allPermissions));

            employeeRepository.save(admin);
        }
    }

    private void createPermissionIfNotExists(String code, String description, String module) {
        if (permissionRepository.findAll().stream().noneMatch(p -> p.getCode().equals(code))) {
            Permission p = new Permission();
            p.setCode(code);
            p.setDescription(description);
            p.setModule(module);
            permissionRepository.save(p);
        }
    }
}
