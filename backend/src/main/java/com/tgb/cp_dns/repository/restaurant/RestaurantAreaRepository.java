package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantArea;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantAreaRepository extends JpaRepository<RestaurantArea, Long> {
    Page<RestaurantArea> findAllByIsDeletedFalse(Pageable pageable);
    boolean existsByNameAndIsDeletedFalse(String name);
    boolean existsByNameAndAreaIdNotAndIsDeletedFalse(String name, Long areaId);
}
