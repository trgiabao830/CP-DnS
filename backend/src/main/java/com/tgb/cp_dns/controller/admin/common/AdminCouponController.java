package com.tgb.cp_dns.controller.admin.common;

import com.tgb.cp_dns.dto.common.CouponRequest;
import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.service.AdminCouponService;
import com.tgb.cp_dns.service.SseNotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService couponService;
    private final SseNotificationService sseService;

    @GetMapping("/sse/subscribe")
    public SseEmitter subscribeToEvents() {
        return sseService.subscribe();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COUPON_VIEW')")
    public ResponseEntity<Page<Coupon>> getCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyActive,
            @PageableDefault(size = 10, sort = { "validUntil",
                    "couponId" }, direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(couponService.getCoupons(keyword, serviceType, status, onlyActive, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COUPON_CREATE')")
    public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_UPDATE')")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('COUPON_UPDATE')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam CouponStatus status) {
        couponService.updateCouponStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_DELETE')")
    public ResponseEntity<String> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok("Xóa mã giảm giá thành công");
    }
}