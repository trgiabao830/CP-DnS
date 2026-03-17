package com.tgb.cp_dns.repository.homestay;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tgb.cp_dns.entity.homestay.HomestayAmenity;
import com.tgb.cp_dns.enums.HomestayCommonStatus;

public interface HomestayAmenityRepository extends JpaRepository<HomestayAmenity, Long> {
        boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

        boolean existsByNameIgnoreCaseAndAmenityIdNotAndIsDeletedFalse(String name, Long id);

        @Query("SELECT a FROM HomestayAmenity a WHERE a.isDeleted = false " +
                        "AND (:keyword IS NULL OR LOWER(a.name) LIKE :keyword) " +
                        "AND (:status IS NULL OR a.status = :status)")
        Page<HomestayAmenity> search(@Param("keyword") String keyword, @Param("status") HomestayCommonStatus status,
                        Pageable pageable);
}
