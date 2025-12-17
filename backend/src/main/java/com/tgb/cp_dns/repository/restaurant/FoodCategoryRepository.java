package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM FoodCategory c")
    Integer findMaxDisplayOrder();

    List<FoodCategory> findAllByIsDeletedFalseOrderByDisplayOrderAsc();
    boolean existsByNameAndIsDeletedFalse(String name);
    boolean existsByNameAndCategoryIdNotAndIsDeletedFalse(String name, Long categoryId);
}
