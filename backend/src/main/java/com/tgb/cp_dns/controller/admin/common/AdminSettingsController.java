package com.tgb.cp_dns.controller.admin.common;

import com.tgb.cp_dns.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingsService settingsService;

    @GetMapping("/restaurant/deposit")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<?> getDepositRequirement() {
        boolean isRequired = settingsService.getDepositRequirement();
        return ResponseEntity.ok(Map.of("isRequired", isRequired));
    }

    @PatchMapping("/restaurant/deposit")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateDepositRequirement(@RequestParam boolean isRequired) {
        settingsService.updateDepositRequirement(isRequired);

        String message = isRequired ? "Đã BẬT yêu cầu đặt cọc." : "Đã TẮT yêu cầu đặt cọc.";
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/restaurant/foods/reset-status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> resetAllOutOfStockFoods() {
        int updatedCount = settingsService.resetAllOutOfStockFoods();

        return ResponseEntity.ok(Map.of(
                "message", "Đã cập nhật thành công " + updatedCount + " món ăn thành trạng thái ĐANG BÁN.",
                "updatedCount", updatedCount));
    }

    @GetMapping("/cancellation-deadlines")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW') or hasAuthority('HOMESTAY_VIEW')")
    public ResponseEntity<?> getCancellationDeadlines() {
        return ResponseEntity.ok(Map.of(
                "homestayHours", settingsService.getHomestayCancellationDeadline(),
                "restaurantDepositHours", settingsService.getRestaurantCancellationDeadlineWithDeposit(),
                "restaurantNoDepositHours", settingsService.getRestaurantCancellationDeadlineNoDeposit()));
    }

    @PatchMapping("/cancellation-deadlines")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE') and hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateCancellationDeadlines(
            @RequestParam(required = false) Integer homestayHours,
            @RequestParam(required = false) Integer restaurantDepositHours,
            @RequestParam(required = false) Integer restaurantNoDepositHours) {

        settingsService.updateCancellationDeadlines(homestayHours, restaurantDepositHours, restaurantNoDepositHours);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thời gian hủy đơn thành công."));
    }

    @GetMapping("/restaurant/operating-hours")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<?> getOperatingHours() {
        return ResponseEntity.ok(Map.of(
                "openingTime", settingsService.getRestaurantOpeningTime().toString(),
                "closingTime", settingsService.getRestaurantClosingTime().toString()));
    }

    @PatchMapping("/restaurant/operating-hours")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateOperatingHours(
            @RequestParam(required = false) String openingTime,
            @RequestParam(required = false) String closingTime) {

        settingsService.updateRestaurantOperatingHours(openingTime, closingTime);
        return ResponseEntity.ok(Map.of("message", "Cập nhật khung giờ hoạt động của nhà hàng thành công."));
    }
}