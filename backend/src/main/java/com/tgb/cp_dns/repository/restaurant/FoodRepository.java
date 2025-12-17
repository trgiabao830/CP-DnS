package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.Food;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodRepository extends JpaRepository<Food, Long> {
    @Query("SELECT COALESCE(MAX(f.displayOrder), 0) FROM Food f WHERE f.category.categoryId = :categoryId")
    Integer findMaxDisplayOrder(@Param("categoryId") Long categoryId);

    Page<Food> findAllByIsDeletedFalse(Pageable pageable);
    List<Food> findByCategory_CategoryIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long categoryId);
    boolean existsByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);
    boolean existsByNameAndIsDeletedFalse(String name);
    boolean existsByNameAndFoodIdNotAndIsDeletedFalse(String name, Long foodId);
}