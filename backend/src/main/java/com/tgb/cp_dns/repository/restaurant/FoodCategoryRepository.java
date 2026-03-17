package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.FoodCategory;
import com.tgb.cp_dns.enums.CategoryStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
        @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM FoodCategory c")
        Integer findMaxDisplayOrder();

        @Query("SELECT c FROM FoodCategory c WHERE " +
                        "c.isDeleted = false AND " +
                        "(:keyword IS NULL OR LOWER(c.name) LIKE :keyword) AND " +
                        "(:status IS NULL OR c.status = :status)")
        Page<FoodCategory> searchCategories(
                        @Param("keyword") String keyword,
                        @Param("status") CategoryStatus status,
                        Pageable pageable);

        boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

        boolean existsByNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(String name, Long categoryId);
}
