package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantTable;
import com.tgb.cp_dns.enums.TableStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
        @Query("SELECT t FROM RestaurantTable t WHERE " +
                        "t.isDeleted = false AND " +
                        "(:areaId IS NULL OR t.area.areaId = :areaId) AND " +
                        "(:keyword IS NULL OR LOWER(t.tableNumber) LIKE :keyword OR LOWER(t.area.name) LIKE :keyword) AND "
                        +
                        "(:status IS NULL OR t.status = :status)")
        Page<RestaurantTable> searchTables(
                        @Param("keyword") String keyword,
                        @Param("status") TableStatus status,
                        @Param("areaId") Long areaId,
                        Pageable pageable);

        boolean existsByArea_AreaIdAndIsDeletedFalse(Long areaId);

        boolean existsByTableNumberIgnoreCaseAndIsDeletedFalse(String tableNumber);

        boolean existsByTableNumberIgnoreCaseAndTableIdNotAndIsDeletedFalse(String tableNumber, Long tableId);

        @Query("SELECT b.table.tableId FROM RestaurantBooking b " +
                        "WHERE b.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                        "AND b.bookingTime < :endTime " +
                        "AND b.endTime > :startTime")
        List<Long> findOccupiedTableIds(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        List<RestaurantTable> findAllByCapacityBetweenAndStatusAndIsDeletedFalseOrderByTableIdAsc(
                        Integer minCapacity, Integer maxCapacity, TableStatus status);

        List<RestaurantTable> findAllByIsDeletedFalse();
}