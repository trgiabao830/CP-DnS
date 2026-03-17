package com.tgb.cp_dns.controller.admin.homestay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgb.cp_dns.dto.homestay.AdminAvailableRoomGroupResponse;
import com.tgb.cp_dns.dto.homestay.AdminHomestayBookingDetailResponse;
import com.tgb.cp_dns.dto.homestay.AdminHomestayBookingResponse;
import com.tgb.cp_dns.dto.homestay.AmenityRequest;
import com.tgb.cp_dns.dto.homestay.ChartDataResponse;
import com.tgb.cp_dns.dto.homestay.CreateHomestayBookingRequest;
import com.tgb.cp_dns.dto.homestay.HomestayOverviewResponse;
import com.tgb.cp_dns.dto.homestay.HomestayStatisticResponse;
import com.tgb.cp_dns.dto.homestay.RoomClassRequest;
import com.tgb.cp_dns.dto.homestay.RoomRequest;
import com.tgb.cp_dns.dto.homestay.RoomTypeRequest;
import com.tgb.cp_dns.dto.restaurant.BookingCreationResponse;
import com.tgb.cp_dns.entity.homestay.HomestayAmenity;
import com.tgb.cp_dns.entity.homestay.HomestayRoomClass;
import com.tgb.cp_dns.entity.homestay.Room;
import com.tgb.cp_dns.entity.homestay.RoomType;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.HomestayCommonStatus;
import com.tgb.cp_dns.enums.RoomStatus;
import com.tgb.cp_dns.service.AdminHomestayService;
import com.tgb.cp_dns.service.SseNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/homestay")
@RequiredArgsConstructor
public class AdminHomestayController {

    private final AdminHomestayService homestayService;
    private final SseNotificationService sseService;

    @GetMapping("/sse/subscribe")
    public SseEmitter subscribeToEvents() {
        return sseService.subscribe();
    }

    @GetMapping("/amenities")
    @PreAuthorize("hasAuthority('HOMESTAY_VIEW')")
    public ResponseEntity<Page<HomestayAmenity>> getAmenities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "amenityId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "amenityId"));
        }

        return ResponseEntity.ok(homestayService.getAmenities(keyword, status, pageable));
    }

    @PostMapping("/amenities")
    @PreAuthorize("hasAuthority('HOMESTAY_CREATE')")
    public ResponseEntity<?> createAmenity(@Valid @RequestBody AmenityRequest request) {
        homestayService.createAmenity(request);
        return ResponseEntity.ok("Tạo tiện ích thành công");
    }

    @PutMapping("/amenities/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateAmenity(@PathVariable Long id, @Valid @RequestBody AmenityRequest request) {
        homestayService.updateAmenity(id, request);
        return ResponseEntity.ok("Cập nhật tiện ích thành công");
    }

    @PatchMapping("/amenities/{id}/status")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateAmenityStatus(@PathVariable Long id, @RequestParam HomestayCommonStatus status) {
        homestayService.updateAmenityStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @DeleteMapping("/amenities/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_DELETE')")
    public ResponseEntity<?> deleteAmenity(@PathVariable Long id) {
        homestayService.deleteAmenity(id);
        return ResponseEntity.ok("Xóa tiện ích thành công");
    }

    @GetMapping("/room-classes")
    @PreAuthorize("hasAuthority('HOMESTAY_VIEW')")
    public ResponseEntity<Page<HomestayRoomClass>> getRoomClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "classId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "classId"));
        }

        return ResponseEntity.ok(homestayService.getRoomClasses(keyword, status, pageable));
    }

    @PostMapping("/room-classes")
    @PreAuthorize("hasAuthority('HOMESTAY_CREATE')")
    public ResponseEntity<?> createRoomClass(@Valid @RequestBody RoomClassRequest request) {
        homestayService.createRoomClass(request);
        return ResponseEntity.ok("Tạo hạng phòng thành công");
    }

    @PutMapping("/room-classes/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoomClass(@PathVariable Long id, @Valid @RequestBody RoomClassRequest request) {
        homestayService.updateRoomClass(id, request);
        return ResponseEntity.ok("Cập nhật hạng phòng thành công");
    }

    @PatchMapping("/room-classes/{id}/status")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoomClassStatus(@PathVariable Long id, @RequestParam HomestayCommonStatus status) {
        homestayService.updateRoomClassStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @DeleteMapping("/room-classes/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_DELETE')")
    public ResponseEntity<?> deleteRoomClass(@PathVariable Long id) {
        homestayService.deleteRoomClass(id);
        return ResponseEntity.ok("Xóa hạng phòng thành công");
    }

    @GetMapping("/room-types")
    @PreAuthorize("hasAuthority('HOMESTAY_VIEW')")
    public ResponseEntity<Page<RoomType>> getRoomTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId,
            @PageableDefault(size = 10, sort = "typeId", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "typeId"));
        }
        return ResponseEntity.ok(homestayService.getRoomTypes(keyword, status, classId, pageable));
    }

    @PostMapping(value = "/room-types", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOMESTAY_CREATE')")
    public ResponseEntity<?> createRoomType(
            @RequestPart("data") String json,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        RoomTypeRequest request = objectMapper.readValue(json, RoomTypeRequest.class);

        homestayService.createRoomType(request, images);
        return ResponseEntity.ok("Tạo loại phòng thành công");
    }

    @PutMapping(value = "/room-types/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoomType(
            @PathVariable Long id,
            @RequestPart("data") String json,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        RoomTypeRequest request = objectMapper.readValue(json, RoomTypeRequest.class);

        homestayService.updateRoomType(id, request, newImages);
        return ResponseEntity.ok("Cập nhật loại phòng thành công");
    }

    @PatchMapping("/room-types/{id}/status")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoomTypeStatus(@PathVariable Long id, @RequestParam HomestayCommonStatus status) {
        homestayService.updateRoomTypeStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @DeleteMapping("/room-types/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_DELETE')")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        homestayService.deleteRoomType(id);
        return ResponseEntity.ok("Xóa loại phòng thành công");
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAuthority('HOMESTAY_VIEW')")
    public ResponseEntity<Page<Room>> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long typeId,
            @PageableDefault(size = 10, sort = "roomNumber", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            pageable = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "roomNumber"));
        }

        return ResponseEntity.ok(homestayService.getRooms(keyword, status, typeId, pageable));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAuthority('HOMESTAY_CREATE')")
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomRequest request) {
        homestayService.createRoom(request);
        return ResponseEntity.ok("Tạo phòng thành công");
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        homestayService.updateRoom(id, request);
        return ResponseEntity.ok("Cập nhật phòng thành công");
    }

    @PatchMapping("/rooms/{id}/status")
    @PreAuthorize("hasAuthority('HOMESTAY_UPDATE')")
    public ResponseEntity<?> updateRoomStatus(@PathVariable Long id, @RequestParam RoomStatus status) {
        homestayService.updateRoomStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái phòng thành công");
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('HOMESTAY_DELETE')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        homestayService.deleteRoom(id);
        return ResponseEntity.ok("Xóa phòng thành công");
    }

    @GetMapping("/booking")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_VIEW')")
    public ResponseEntity<Page<AdminHomestayBookingResponse>> getBookingList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                homestayService.getHomestayBookings(keyword, status, fromDate, toDate, pageable));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_VIEW')")
    public ResponseEntity<AdminHomestayBookingDetailResponse> getBookingDetail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(homestayService.getHomestayBookingDetail(bookingId));
    }

    @PutMapping("/booking/{bookingId}/status")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_UPDATE')")
    public ResponseEntity<AdminHomestayBookingDetailResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam BookingStatus status,
            @RequestParam(required = false) LocalDate confirmCheckOutDate,
            @RequestParam(required = false) String couponCode) {

        return ResponseEntity.ok(
                homestayService.updateHomestayBookingStatus(bookingId, status, confirmCheckOutDate, couponCode));
    }

    @PostMapping("/booking/{bookingId}/cancel")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_UPDATE')")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        homestayService.cancelHomestayBookingByAdmin(bookingId);
        return ResponseEntity.ok("Đã hủy đơn đặt phòng thành công.");
    }

    @GetMapping("/pos/overview")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_VIEW')")
    public ResponseEntity<HomestayOverviewResponse> getPosOverview() {
        return ResponseEntity.ok(homestayService.getHomestayPosOverview());
    }

    @PostMapping("/booking/walk-in")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_CREATE')")
    public ResponseEntity<BookingCreationResponse> createWalkInBooking(
            @RequestBody CreateHomestayBookingRequest request) {
        return ResponseEntity.ok(homestayService.createWalkInHomestayBooking(request));
    }

    @GetMapping("/booking/{bookingId}/available-rooms")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_UPDATE')")
    public ResponseEntity<List<AdminAvailableRoomGroupResponse>> getAvailableRoomsForChange(
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(homestayService.getAvailableRoomsForChange(bookingId));
    }

    @GetMapping("/available-rooms-public")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_CREATE')")
    public ResponseEntity<List<AdminAvailableRoomGroupResponse>> getAvailableRoomsPublic(
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut) {

        return ResponseEntity.ok(homestayService.getAvailableRoomsPublic(checkIn, checkOut));
    }

    @PutMapping("/booking/{bookingId}/change-room")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_UPDATE')")
    public ResponseEntity<AdminHomestayBookingDetailResponse> changeRoom(
            @PathVariable Long bookingId,
            @RequestParam Long newRoomId,
            @RequestParam(required = false) String couponCode) {
        return ResponseEntity.ok(homestayService.changeRoom(bookingId, newRoomId, couponCode));
    }

    @GetMapping("/statistics/revenue")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_VIEW')")
    public ResponseEntity<HomestayStatisticResponse> getRevenueStatistics() {
        return ResponseEntity.ok(homestayService.getRevenueStatistics());
    }

    @GetMapping("/statistics/chart")
    @PreAuthorize("hasAuthority('HOMESTAY_BOOKING_VIEW')")
    public ResponseEntity<List<ChartDataResponse>> getRevenueChart(
            @RequestParam(defaultValue = "this_week") String type) {
        return ResponseEntity.ok(homestayService.getRevenueChart(type));
    }
}
