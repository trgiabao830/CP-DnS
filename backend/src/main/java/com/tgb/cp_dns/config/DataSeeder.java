package com.tgb.cp_dns.config;

import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.Permission;
import com.tgb.cp_dns.entity.common.SystemConfig;
import com.tgb.cp_dns.enums.EmployeeStatus;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.PermissionRepository;
import com.tgb.cp_dns.repository.common.SystemConfigRepository;

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
    private final SystemConfigRepository systemConfigRepository;

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
        createPermissionIfNotExists("USER_UPDATE", "Cập nhật thông tin khách hàng", "USER");
        createPermissionIfNotExists("USER_LOCK", "Khóa tài khoản khách hàng", "USER");
        createPermissionIfNotExists("USER_DELETE", "Xóa tài khoản khách hàng", "USER");

        createPermissionIfNotExists("RESTAURANT_VIEW", "Xem danh sách bàn/món ăn", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_CREATE", "Tạo bàn/món ăn mới", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_UPDATE", "Cập nhật bàn/món ăn", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_DELETE", "Xóa bàn/món ăn", "RESTAURANT");

        createPermissionIfNotExists("RESTAURANT_BOOKING_VIEW", "Xem danh sách đơn đặt bàn", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_BOOKING_CREATE", "Tạo đơn đặt bàn mới", "RESTAURANT");
        createPermissionIfNotExists("RESTAURANT_BOOKING_UPDATE", "Cập nhật đơn đặt bàn", "RESTAURANT");

        createPermissionIfNotExists("HOMESTAY_VIEW", "Xem danh sách phòng, loại phòng, tiện ích", "HOMESTAY");
        createPermissionIfNotExists("HOMESTAY_CREATE", "Tạo mới phòng, loại phòng, tiện ích", "HOMESTAY");
        createPermissionIfNotExists("HOMESTAY_UPDATE", "Cập nhật thông tin phòng/homestay", "HOMESTAY");
        createPermissionIfNotExists("HOMESTAY_DELETE", "Xóa dữ liệu phòng/homestay", "HOMESTAY");

        createPermissionIfNotExists("HOMESTAY_BOOKING_VIEW", "Xem danh sách đơn phòng", "HOMESTAY");
        createPermissionIfNotExists("HOMESTAY_BOOKING_CREATE", "Tạo đơn phòng mới", "HOMESTAY");
        createPermissionIfNotExists("HOMESTAY_BOOKING_UPDATE", "Cập nhật đơn phòng", "HOMESTAY");

        createPermissionIfNotExists("COUPON_VIEW", "Xem danh sách mã giảm giá", "COUPON");
        createPermissionIfNotExists("COUPON_CREATE", "Tạo mã giảm giá mới", "COUPON");
        createPermissionIfNotExists("COUPON_UPDATE", "Cập nhật mã giảm giá", "COUPON");
        createPermissionIfNotExists("COUPON_DELETE", "Xóa mã giảm giá", "COUPON");

        createPermissionIfNotExists("SUPPORT_VIEW", "Xem danh sách hội thoại và lịch sử chat", "SUPPORT");
        createPermissionIfNotExists("SUPPORT_ACTION", "Tiếp nhận và xử lý hội thoại (Chat)", "SUPPORT");

        createConfigIfNotExists("BOOKING_REQUIRE_DEPOSIT_NO_FOOD", "true",
                "Yêu cầu đặt cọc khi chỉ đặt bàn mà không gọi món trước");

        createConfigIfNotExists("HOMESTAY_CANCELLATION_DEADLINE_HOURS", "48",
                "Thời gian tối thiểu (giờ) khách được phép hủy đơn đặt phòng miễn phí trước giờ nhận phòng");

        createConfigIfNotExists("RESTAURANT_CANCELLATION_DEADLINE_HOURS_WITH_DEPOSIT", "48",
                "Thời gian tối thiểu (giờ) khách được phép hủy đơn đặt bàn CÓ CỌC/GỌI MÓN miễn phí trước giờ nhận bàn");

        createConfigIfNotExists("RESTAURANT_CANCELLATION_DEADLINE_HOURS_NO_DEPOSIT", "1",
                "Thời gian tối thiểu (giờ) khách được phép hủy đơn đặt bàn KHÔNG CỌC trước giờ nhận bàn");

        createConfigIfNotExists("RESTAURANT_OPENING_TIME", "08:00",
                "Thời gian nhà hàng bắt đầu nhận khách đặt bàn (định dạng HH:mm)");

        createConfigIfNotExists("RESTAURANT_CLOSING_TIME", "21:30",
                "Thời gian nhà hàng kết thúc nhận khách đặt bàn (định dạng HH:mm)");

        if (!employeeRepository.existsByUsernameAndIsDeletedFalse(adminUsername)) {
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

    private void createConfigIfNotExists(String key, String value, String description) {
        if (!systemConfigRepository.existsById(key)) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            systemConfigRepository.save(config);
        }
    }
}
