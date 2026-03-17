package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.Food;
import com.tgb.cp_dns.enums.FoodStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodRepository extends JpaRepository<Food, Long> {

        @Override
        @EntityGraph(attributePaths = { "variants", "variants.options", "category" })
        Optional<Food> findById(Long id);

        @Query("SELECT DISTINCT f FROM Food f " +
                        "LEFT JOIN FETCH f.variants v " +
                        "WHERE f.foodId IN :ids")
        List<Food> findAllWithDetailsByIds(@Param("ids") Collection<Long> ids);

        @Query("SELECT COALESCE(MAX(f.displayOrder), 0) FROM Food f WHERE f.category.categoryId = :categoryId")
        Integer findMaxDisplayOrder(@Param("categoryId") Long categoryId);

        Page<Food> findAllByIsDeletedFalse(Pageable pageable);

        List<Food> findByCategory_CategoryIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long categoryId);

        boolean existsByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);

        boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

        boolean existsByNameIgnoreCaseAndFoodIdNotAndIsDeletedFalse(String name, Long foodId);

        List<Food> findAllByStatusAndIsDeletedFalse(FoodStatus status);

        @Query("SELECT f FROM Food f " +
                        "LEFT JOIN FETCH f.category c " +
                        "WHERE f.isDeleted = false " +
                        "AND f.status IN :statuses " +
                        "AND (:keyword IS NULL OR LOWER(f.name) LIKE :keyword) " +
                        "ORDER BY c.displayOrder ASC, f.displayOrder ASC")
        List<Food> searchFoodsForBooking(@Param("keyword") String keyword,
                        @Param("statuses") List<FoodStatus> statuses);

        @Query("SELECT f FROM Food f WHERE " +
                        "f.category.categoryId = :categoryId AND " +
                        "f.isDeleted = false AND " +
                        "(:keyword IS NULL OR LOWER(f.name) LIKE :keyword) AND " +
                        "(:status IS NULL OR f.status = :status)")
        Page<Food> searchFoodsByCategory(
                        @Param("categoryId") Long categoryId,
                        @Param("keyword") String keyword,
                        @Param("status") FoodStatus status,
                        Pageable pageable);

        @Query("SELECT f FROM Food f WHERE " +
                        "f.isDeleted = false AND " +
                        "(:categoryId IS NULL OR f.category.categoryId = :categoryId) AND " +
                        "(:keyword IS NULL OR LOWER(f.name) LIKE :keyword) AND " +
                        "(:status IS NULL OR f.status = :status)")
        Page<Food> searchFoods(
                        @Param("categoryId") Long categoryId,
                        @Param("keyword") String keyword,
                        @Param("status") FoodStatus status,
                        Pageable pageable);

        @Query("SELECT DISTINCT f.name FROM Food f " +
                        "JOIN f.variants v " +
                        "JOIN v.options o " +
                        "WHERE o.linkedFood.foodId = :foodId " +
                        "AND f.isDeleted = false " +
                        "AND v.isDeleted = false " +
                        "AND o.isDeleted = false")
        List<String> findComboNamesByLinkedFoodId(@Param("foodId") Long foodId);
}