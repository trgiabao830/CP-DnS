package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.UserCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCouponUsageRepository extends JpaRepository<UserCouponUsage, Long> {
}
