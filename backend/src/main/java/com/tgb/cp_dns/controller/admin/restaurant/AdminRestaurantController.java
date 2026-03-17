package com.tgb.cp_dns.controller.admin.restaurant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgb.cp_dns.dto.restaurant.*;
import com.tgb.cp_dns.entity.restaurant.*;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.service.AdminRestaurantService;
import com.tgb.cp_dns.service.ClientBookingService;
import com.tgb.cp_dns.service.SseNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/restaurant")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final AdminRestaurantService restaurantService;
    private final ClientBookingService clientBookingService;
    private final SseNotificationService sseService;

    @GetMapping("/sse/subscribe")
    public SseEmitter subscribeToEvents() {
        return sseService.subscribe();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<FoodCategory>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "displayOrder"));
        }

        return ResponseEntity.ok(restaurantService.getAllCategories(keyword, status, pageable));
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
    public ResponseEntity<Page<FoodResponse>> getFoodsByCategory(
            @PathVariable("id") Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "displayOrder"));
        }

        return ResponseEntity.ok(restaurantService.getFoodsByCategory(categoryId, keyword, status, pageable));
    }

    @PatchMapping("/categories/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateCategoryStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateCategoryStatusRequest request) {
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
    public ResponseEntity<Page<RestaurantArea>> getAreas(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "areaId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {
        if (unpaged) {
            pageable = Pageable.unpaged(pageable.getSort());
        }

        return ResponseEntity.ok(restaurantService.getAllAreas(keyword, status, pageable));
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
    public ResponseEntity<?> updateAreaStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateAreaStatusRequest request) {
        restaurantService.updateAreaStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái khu vực thành công");
    }

    @GetMapping("/tables")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<RestaurantTable>> getTables(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long areaId,
            @PageableDefault(size = 10, sort = "tableNumber") Pageable pageable) {

        return ResponseEntity.ok(restaurantService.getAllTables(keyword, status, areaId, pageable));
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
    public ResponseEntity<?> updateTableStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateTableStatusRequest request) {
        restaurantService.updateTableStatus(id, request.getStatus());
        return ResponseEntity.ok("Cập nhật trạng thái bàn thành công");
    }

    @GetMapping("/foods")
    @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public ResponseEntity<Page<FoodResponse>> getAllFoods(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10, sort = { "category.displayOrder",
                    "displayOrder" }, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getAllFoods(keyword, status, categoryId, pageable));
    }

    @GetMapping("/foods/{id}")
    public ResponseEntity<FoodResponse> getFoodDetail(@PathVariable Long id) {
        FoodResponse response = restaurantService.getFoodDetail(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/foods", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createFood(
            @RequestPart("data") String foodRequestJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        FoodRequest request = objectMapper.readValue(foodRequestJson, FoodRequest.class);

        restaurantService.createFood(request, image);

        return ResponseEntity.ok("Tạo món ăn thành công");

    }

    @PutMapping(value = "/foods/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateFood(
            @PathVariable Long id,
            @RequestPart("data") String foodRequestJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        FoodRequest request = objectMapper.readValue(foodRequestJson, FoodRequest.class);

        restaurantService.updateFood(id, request, imageFile);

        return ResponseEntity.ok("Cập nhật món ăn thành công");
    }

    @PatchMapping("/foods/{id}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_UPDATE')")
    public ResponseEntity<?> updateFoodStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateFoodStatusRequest request) {
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

    @GetMapping("/booking")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_VIEW')")
    public ResponseEntity<Page<AdminBookingResponse>> getBookingList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "bookingTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                restaurantService.getBookings(keyword, status, fromDate, toDate, pageable));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_VIEW')")
    public ResponseEntity<AdminBookingDetailResponse> getBookingDetail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(restaurantService.getBookingDetail(bookingId));
    }

    @PutMapping("/booking/{bookingId}/cancel")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_BOOKING_UPDATE')")
    public ResponseEntity<String> cancelBookingByAdmin(@PathVariable Long bookingId) {

        restaurantService.cancelBookingByAdmin(bookingId);

        return ResponseEntity.ok("Hủy đơn đặt bàn thành công.");
    }

    @PutMapping("/booking/{bookingId}/status")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_UPDATE')")
    public ResponseEntity<AdminBookingDetailResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam BookingStatus status) {
        return ResponseEntity.ok(restaurantService.updateBookingStatus(bookingId, status));
    }

    @PostMapping("/booking/create")
    @PreAuthorize("hasAuthority('RESTAURANT_CREATE')")
    public ResponseEntity<?> createBookingForCustomer(
            @RequestBody @Valid CreateBookingRequest request,
            HttpServletRequest httpReq) {

        String ipAddress = httpReq.getRemoteAddr();

        BookingCreationResponse response = clientBookingService.createBooking(request, ipAddress);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/walk-in")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_CREATE')")
    public ResponseEntity<AdminBookingDetailResponse> createWalkIn(@RequestBody WalkInBookingRequest req) {
        return ResponseEntity.ok(restaurantService.createWalkInBooking(req));
    }

    @PostMapping("/bookings/{bookingId}/items")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_UPDATE')")
    public ResponseEntity<AdminBookingDetailResponse> addOrderItem(
            @PathVariable Long bookingId,
            @RequestBody OrderItemRequest itemReq) {
        return ResponseEntity.ok(restaurantService.addOrderItem(bookingId, itemReq));
    }

    @PutMapping("/bookings/{bookingId}/items/{detailId}")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_UPDATE')")
    public ResponseEntity<AdminBookingDetailResponse> updateItemQuantity(
            @PathVariable Long bookingId,
            @PathVariable Long detailId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(restaurantService.updateOrderItemQuantity(bookingId, detailId, quantity));
    }

    @PutMapping("/bookings/{bookingId}/move-table")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_BOOKING_UPDATE')")
    public ResponseEntity<AdminBookingDetailResponse> moveTable(
            @PathVariable Long bookingId,
            @RequestParam Long newTableId) {

        return ResponseEntity.ok(restaurantService.moveBookingTable(bookingId, newTableId));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('RESTAURANT_BOOKING_VIEW')")
    public ResponseEntity<RestaurantOverviewResponse> getOverview() {
        return ResponseEntity.ok(restaurantService.getDailyOverview());
    }

    @GetMapping("/statistics/revenue")
    public ResponseEntity<RestaurantStatisticResponse> getRevenueStatistics() {
        return ResponseEntity.ok(restaurantService.getRevenueStatistics());
    }

    @GetMapping("/statistics/chart")
    public ResponseEntity<List<ChartDataResponse>> getRevenueChart(
            @RequestParam(defaultValue = "this_week") String type) {
        return ResponseEntity.ok(restaurantService.getRevenueChart(type));
    }
}