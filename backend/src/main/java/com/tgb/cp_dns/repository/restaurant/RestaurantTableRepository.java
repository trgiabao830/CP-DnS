package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantTable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    Page<RestaurantTable> findAllByIsDeletedFalse(Pageable pageable);
    boolean existsByArea_AreaIdAndIsDeletedFalse(Long areaId);
    boolean existsByTableNumberAndIsDeletedFalse(String tableNumber);
    boolean existsByTableNumberAndTableIdNotAndIsDeletedFalse(String tableNumber, Long tableId);
}