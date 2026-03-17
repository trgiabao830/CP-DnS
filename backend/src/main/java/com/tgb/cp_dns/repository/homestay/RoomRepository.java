package com.tgb.cp_dns.repository.homestay;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tgb.cp_dns.entity.homestay.Room;
import com.tgb.cp_dns.enums.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {
        @Query("SELECT r FROM Room r WHERE r.isDeleted = false " +
                        "AND (:typeId IS NULL OR r.roomType.typeId = :typeId) " +
                        "AND (:keyword IS NULL OR LOWER(r.roomNumber) LIKE :keyword OR LOWER(r.roomType.name) LIKE :keyword) "
                        +
                        "AND (:status IS NULL OR r.status = :status)")
        Page<Room> search(
                        @Param("keyword") String keyword,
                        @Param("status") RoomStatus status,
                        @Param("typeId") Long typeId,
                        Pageable pageable);

        Optional<Room> findByRoomIdAndIsDeletedFalse(Long roomId);

        boolean existsByRoomNumberIgnoreCaseAndIsDeletedFalse(String roomNumber);

        boolean existsByRoomNumberIgnoreCaseAndRoomIdNotAndIsDeletedFalse(String roomNumber, Long id);

        boolean existsByRoomType_TypeIdAndIsDeletedFalse(Long typeId);

        List<Room> findAllByStatusAndIsDeletedFalse(RoomStatus status);

        List<Room> findAllByRoomType_TypeIdAndStatusAndIsDeletedFalse(Long typeId, RoomStatus status);

        List<Room> findAllByIsDeletedFalse();
}
