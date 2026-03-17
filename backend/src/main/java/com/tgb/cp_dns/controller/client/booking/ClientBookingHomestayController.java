package com.tgb.cp_dns.controller.client.booking;

import com.tgb.cp_dns.dto.homestay.HomestaySearchRequest;
import com.tgb.cp_dns.dto.homestay.RoomTypeDetailResponse;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.dto.common.CouponResponse;
import com.tgb.cp_dns.dto.homestay.AvailableRoomTypeResponse;
import com.tgb.cp_dns.dto.homestay.CreateHomestayBookingRequest;
import com.tgb.cp_dns.dto.homestay.HomestayBookingDetailResponse;
import com.tgb.cp_dns.dto.homestay.HomestayBookingSummaryResponse;
import com.tgb.cp_dns.service.ClientBookingHomestayService;
import com.tgb.cp_dns.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homestay/booking")
@RequiredArgsConstructor
public class ClientBookingHomestayController {

    private final ClientBookingHomestayService clientHomestayService;
    private final PaymentService paymentService;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    @PostMapping("/search")
    public ResponseEntity<List<AvailableRoomTypeResponse>> searchAvailableRooms(
            @RequestBody HomestaySearchRequest request) {

        List<AvailableRoomTypeResponse> responses = clientHomestayService.searchAvailableRoomTypes(request);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/room-types/{id}")
    public ResponseEntity<RoomTypeDetailResponse> getRoomTypeDetail(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut) {

        return ResponseEntity.ok(clientHomestayService.getRoomTypeDetail(id, checkIn, checkOut));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @RequestBody @Valid CreateHomestayBookingRequest request,
            HttpServletRequest httpReq) {

        String ipAddress = httpReq.getRemoteAddr();

        return ResponseEntity.ok(clientHomestayService.createHomestayBooking(request, ipAddress));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons() {
        return ResponseEntity.ok(clientHomestayService.getAvailableHomestayCoupons());
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            HomestayBooking booking = (HomestayBooking) paymentService.processVnPayCallback(params);

            String accessToken = booking.getAccessToken();
            String txnCode = params.get("vnp_TxnRef");
            String frontendUrl;

            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                frontendUrl = frontendBaseUrl + "/homestay/booking/success"
                        + "?code=" + txnCode
                        + "&accessToken=" + accessToken;
            } else {
                frontendUrl = frontendBaseUrl + "/homestay/booking/failed"
                        + "?error=" + URLEncoder.encode("Thanh toán thất bại hoặc bị hủy", StandardCharsets.UTF_8);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl))
                    .build();

        } catch (Exception e) {
            String failedUrl = frontendBaseUrl + "/homestay/booking/failed?error="
                    + URLEncoder.encode("Lỗi hệ thống", StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(failedUrl)).build();
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Page<HomestayBookingSummaryResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(clientHomestayService.getMyHomestayBookings(status, page, size));
    }

    @GetMapping("/view/{accessToken}")
    public ResponseEntity<HomestayBookingDetailResponse> getBookingDetail(
            @PathVariable String accessToken) {

        return ResponseEntity.ok(clientHomestayService.getHomestayBookingDetail(accessToken));
    }

    @PostMapping("/cancel/{accessToken}")
    public ResponseEntity<String> cancelBooking(@PathVariable String accessToken) {
        clientHomestayService.cancelHomestayBooking(accessToken);
        return ResponseEntity.ok("Đã hủy đơn đặt phòng thành công và thực hiện quy trình hoàn tiền (nếu có).");
    }
}