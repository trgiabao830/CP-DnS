package com.tgb.cp_dns.controller.admin.restaurant;

import com.tgb.cp_dns.dto.restaurant.*;
import com.tgb.cp_dns.entity.restaurant.*;
import com.tgb.cp_dns.service.AdminRestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/restaurant")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final AdminRestaurantService restaurantService;

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<List<FoodCategory>> getCategories() {
        return ResponseEntity.ok(restaurantService.getAllCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request) {
        restaurantService.createCategory(request.getName());
        return ResponseEntity.ok("Tạo danh mục thành công");
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        restaurantService.updateCategory(id, request);
        return ResponseEntity.ok("Cập nhật danh mục thành công");
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_DELETE')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        restaurantService.deleteCategory(id);
        return ResponseEntity.ok("Xóa danh mục thành công");
    }

    @GetMapping("/categories/{id}/foods")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<List<FoodResponse>> getFoodsByCategory(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getFoodsByCategory(id));
    }

    @PatchMapping("/categories/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateCategoryStatus(@PathVariable Long id, @Valid @RequestBody UpdateCategoryStatusRequest request) {
        restaurantService.updateCategoryStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái danh mục thành công");
    }

    @PutMapping("/categories/reorder")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> reorderCategories(@RequestBody List<ReorderItem> items) {
        restaurantService.reorderCategories(items);
        return ResponseEntity.ok("Cập nhật thứ tự danh mục thành công");
    }

    @GetMapping("/areas")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<RestaurantArea>> getAreas(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getAllAreas(pageable));
    }

    @PostMapping("/areas")
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createArea(@Valid @RequestBody RestaurantAreaRequest request) {
        restaurantService.createArea(request);
        return ResponseEntity.ok("Tạo khu vực thành công");
    }

    @PutMapping("/areas/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateArea(@PathVariable Long id, @Valid @RequestBody RestaurantAreaRequest request) {
        restaurantService.updateArea(id, request);
        return ResponseEntity.ok("Cập nhật khu vực thành công");
    }

    @DeleteMapping("/areas/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_DELETE')")
    public ResponseEntity<?> deleteArea(@PathVariable Long id) {
        restaurantService.deleteArea(id);
        return ResponseEntity.ok("Xóa khu vực thành công");
    }

    @PatchMapping("/areas/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateAreaStatus(@PathVariable Long id, @Valid @RequestBody UpdateAreaStatusRequest request) {
        restaurantService.updateAreaStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái khu vực thành công");
    }

    @GetMapping("/tables")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<RestaurantTable>> getTables(@PageableDefault(size = 20, sort = "tableNumber") Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getAllTables(pageable));
    }

    @PostMapping("/tables")
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createTable(@Valid @RequestBody RestaurantTableRequest request) {
        restaurantService.createTable(request);
        return ResponseEntity.ok("Tạo bàn ăn thành công");
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_DELETE')")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        restaurantService.deleteTable(id);
        return ResponseEntity.ok("Xóa bàn ăn thành công");
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @Valid @RequestBody RestaurantTableRequest request) {
        restaurantService.updateTable(id, request);
        return ResponseEntity.ok("Cập nhật thông tin bàn thành công");
    }

    @PatchMapping("/tables/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateTableStatus(@PathVariable Long id, @Valid @RequestBody UpdateTableStatusRequest request) {
        restaurantService.updateTableStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái bàn thành công");
    }

    @GetMapping("/foods")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<FoodResponse>> getAllFoods(
        @PageableDefault(size = 10, sort = {"category.displayOrder", "displayOrder"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getAllFoods(pageable));
    }

    @PostMapping("/foods")
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createFood(@Valid @RequestBody FoodRequest request) {
        restaurantService.createFood(request);
        return ResponseEntity.ok("Tạo món ăn thành công");
    }
    
    @PutMapping("/foods/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateFood(@PathVariable Long id, @Valid @RequestBody FoodRequest request) {
        restaurantService.updateFood(id, request);
        return ResponseEntity.ok("Cập nhật món ăn thành công");
    }

    @PatchMapping("/foods/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateFoodStatus(@PathVariable Long id, @Valid @RequestBody UpdateFoodStatusRequest request) {
        restaurantService.updateFoodStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái món ăn thành công");
    }

    @DeleteMapping("/foods/{id}")
    @PreAuthorize("hasAuthority('RESTAURANT_DELETE')")
    public ResponseEntity<?> deleteFood(@PathVariable Long id) {
        restaurantService.deleteFood(id);
        return ResponseEntity.ok("Xóa món ăn thành công");
    }

    @PutMapping("/foods/reorder")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> reorderFoods(@RequestBody List<ReorderItem> items) {
        restaurantService.reorderFoods(items);
        return ResponseEntity.ok("Cập nhật thứ tự món ăn thành công");
    }
}