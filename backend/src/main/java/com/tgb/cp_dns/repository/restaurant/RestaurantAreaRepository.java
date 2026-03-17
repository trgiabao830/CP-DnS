package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantArea;
import com.tgb.cp_dns.enums.AreaStatus;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantAreaRepository extends JpaRepository<RestaurantArea, Long> {
    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndAreaIdNotAndIsDeletedFalse(String name, Long areaId);

    @Query("SELECT a FROM RestaurantArea a WHERE " +
            "a.isDeleted = false AND " +
            "(:keyword IS NULL OR LOWER(a.name) LIKE :keyword) AND " +
            "(:status IS NULL OR a.status = :status)")
    Page<RestaurantArea> searchAreas(
            @Param("keyword") String keyword,
            @Param("status") AreaStatus status,
            Pageable pageable);

    List<RestaurantArea> findAllByIsDeletedFalse();
}
