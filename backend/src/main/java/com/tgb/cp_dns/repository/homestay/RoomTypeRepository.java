package com.tgb.cp_dns.repository.homestay;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tgb.cp_dns.entity.homestay.RoomType;
import com.tgb.cp_dns.enums.HomestayCommonStatus;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
        @Query("SELECT t FROM RoomType t WHERE t.isDeleted = false " +
                        "AND (:classId IS NULL OR t.roomClass.classId = :classId) " +
                        "AND (:keyword IS NULL OR LOWER(t.name) LIKE :keyword) " +
                        "AND (:status IS NULL OR t.status = :status)")
        Page<RoomType> search(
                        @Param("keyword") String keyword,
                        @Param("status") HomestayCommonStatus status,
                        @Param("classId") Long classId,
                        Pageable pageable);

        Optional<RoomType> findByTypeIdAndIsDeletedFalse(Long typeId);

        boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

        boolean existsByNameIgnoreCaseAndTypeIdNotAndIsDeletedFalse(String name, Long id);

        boolean existsByRoomClass_ClassIdAndIsDeletedFalse(Long classId);

        List<RoomType> findAllByStatusAndIsDeletedFalse(HomestayCommonStatus status);

        List<RoomType> findAllByIsDeletedFalse();

}