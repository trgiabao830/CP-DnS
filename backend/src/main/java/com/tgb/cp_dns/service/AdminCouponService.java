package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.common.CouponRequest;
import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.enums.ServiceType;
import com.tgb.cp_dns.repository.common.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final SseNotificationService sseService;

    @Transactional(readOnly = true)
    public Page<Coupon> getCoupons(
            String keyword,
            String serviceTypeStr,
            String statusStr,
            Boolean onlyActive,
            Pageable pageable) {
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        ServiceType type = null;
        if (serviceTypeStr != null && !serviceTypeStr.isEmpty()) {
            try {
                type = ServiceType.valueOf(serviceTypeStr.toUpperCase());
            } catch (Exception e) {
            }
        }

        CouponStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = CouponStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception e) {
            }
        }

        boolean isActiveFilter = Boolean.TRUE.equals(onlyActive);

        LocalDateTime now = LocalDateTime.now();

        return couponRepository.searchCoupons(
                finalKeyword,
                type,
                status,
                isActiveFilter,
                now,
                pageable);
    }

    @Transactional
    public Coupon createCoupon(CouponRequest req) {
        if (couponRepository.existsByCodeIgnoreCaseAndIsDeletedFalse(req.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã giảm giá này đã tồn tại.");
        }

        validateCouponLogic(req);

        Coupon coupon = new Coupon();
        mapRequestToEntity(coupon, req);
        coupon.setUsedCount(0);
        coupon.setDeleted(false);

        Coupon saved = couponRepository.save(coupon);
        sseService.sendNotification("COUPON_UPDATE", saved.getCouponId());
        return saved;
    }

    @Transactional
    public Coupon updateCoupon(Long id, CouponRequest req) {
        Coupon coupon = couponRepository.findByCouponIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        if (couponRepository.existsByCodeIgnoreCaseAndCouponIdNotAndIsDeletedFalse(req.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã giảm giá này đã được sử dụng.");
        }

        validateCouponLogic(req);

        mapRequestToEntity(coupon, req);
        Coupon saved = couponRepository.save(coupon);

        sseService.sendNotification("COUPON_UPDATE", saved.getCouponId());
        return saved;
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findByCouponIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        coupon.setDeleted(true);
        coupon.setStatus(CouponStatus.UNAVAILABLE);

        couponRepository.save(coupon);
        sseService.sendNotification("COUPON_UPDATE", id);
    }

    @Transactional
    public void updateCouponStatus(Long id, CouponStatus status) {
        Coupon coupon = couponRepository.findByCouponIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã giảm giá không tồn tại"));

        coupon.setStatus(status);
        couponRepository.save(coupon);
        sseService.sendNotification("COUPON_UPDATE", id);
    }

    private void validateCouponLogic(CouponRequest req) {
        if (req.getValidUntil().isBefore(req.getValidFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu.");
        }
        boolean hasPercent = req.getDiscountPercent() != null && req.getDiscountPercent() > 0;
        boolean hasAmount = req.getDiscountAmount() != null && req.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;

        if (hasPercent && hasAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được chọn giảm theo % HOẶC tiền mặt.");
        }
    }

    private void mapRequestToEntity(Coupon c, CouponRequest req) {
        c.setCode(req.getCode().toUpperCase());
        c.setDiscountPercent(req.getDiscountPercent());
        c.setDiscountAmount(req.getDiscountAmount());
        c.setMaxDiscountAmount(req.getMaxDiscountAmount());
        c.setMinOrderValue(req.getMinOrderValue());
        c.setQuantity(req.getQuantity());
        c.setServiceType(req.getServiceType());
        c.setValidFrom(req.getValidFrom());
        c.setValidUntil(req.getValidUntil());
        c.setRequireAccount(req.isRequireAccount());
        c.setStatus(req.getStatus());
    }
}