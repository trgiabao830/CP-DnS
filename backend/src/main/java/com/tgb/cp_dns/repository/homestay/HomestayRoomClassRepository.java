package com.tgb.cp_dns.repository.homestay;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tgb.cp_dns.entity.homestay.HomestayRoomClass;
import com.tgb.cp_dns.enums.HomestayCommonStatus;

public interface HomestayRoomClassRepository extends JpaRepository<HomestayRoomClass, Long> {

        boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

        boolean existsByNameIgnoreCaseAndClassIdNotAndIsDeletedFalse(String name, Long id);

        @Query("SELECT c FROM HomestayRoomClass c WHERE c.isDeleted = false " +
                        "AND (:keyword IS NULL OR LOWER(c.name) LIKE :keyword) " +
                        "AND (:status IS NULL OR c.status = :status)")
        Page<HomestayRoomClass> search(@Param("keyword") String keyword, @Param("status") HomestayCommonStatus status,
                        Pageable pageable);

        List<HomestayRoomClass> findAllByIsDeletedFalse();
}
