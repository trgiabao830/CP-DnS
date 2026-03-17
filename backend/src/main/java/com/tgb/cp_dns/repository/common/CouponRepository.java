package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.enums.ServiceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

        Optional<Coupon> findByCodeIgnoreCaseAndIsDeletedFalse(String code);

        boolean existsByCodeIgnoreCaseAndIsDeletedFalse(String code);

        boolean existsByCodeIgnoreCaseAndCouponIdNotAndIsDeletedFalse(String code, Long id);

        Optional<Coupon> findByCouponIdAndIsDeletedFalse(Long id);

        @Query("SELECT c FROM Coupon c WHERE " +
                        "c.isDeleted = false " +
                        "AND (:keyword IS NULL OR LOWER(c.code) LIKE :keyword) " +
                        "AND (:serviceType IS NULL OR c.serviceType = :serviceType) " +
                        "AND (:status IS NULL OR c.status = :status) " +
                        "AND (:onlyActive = false OR " +
                        "    (c.status = 'AVAILABLE' " +
                        "     AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
                        "     AND (c.validUntil IS NULL OR c.validUntil >= :now) " +
                        "     AND (c.quantity IS NULL OR c.usedCount < c.quantity)))")
        Page<Coupon> searchCoupons(
                        @Param("keyword") String keyword,
                        @Param("serviceType") ServiceType serviceType,
                        @Param("status") CouponStatus status,
                        @Param("onlyActive") boolean onlyActive,
                        @Param("now") LocalDateTime now,
                        Pageable pageable);

        @Query("SELECT c FROM Coupon c WHERE " +
                        "c.isDeleted = false " +
                        "AND c.status = 'AVAILABLE' " +
                        "AND c.serviceType = :serviceType " +
                        "AND (c.validFrom IS NULL OR c.validFrom <= :now) " +
                        "AND (c.validUntil IS NULL OR c.validUntil >= :now) " +
                        "AND (c.quantity IS NULL OR c.usedCount < c.quantity)")
        List<Coupon> findAvailableCoupons(@Param("serviceType") ServiceType serviceType,
                        @Param("now") LocalDateTime now);
}
