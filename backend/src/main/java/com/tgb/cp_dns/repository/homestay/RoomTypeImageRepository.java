package com.tgb.cp_dns.repository.homestay;

import com.tgb.cp_dns.entity.homestay.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {

    List<RoomTypeImage> findByRoomType_TypeIdOrderByDisplayOrderAsc(Long typeId);

    Optional<RoomTypeImage> findFirstByRoomType_TypeIdOrderByDisplayOrderAsc(Long typeId);

    @Query("SELECT MAX(i.displayOrder) FROM RoomTypeImage i WHERE i.roomType.typeId = :typeId")
    Integer findMaxDisplayOrder(@Param("typeId") Long typeId);
}