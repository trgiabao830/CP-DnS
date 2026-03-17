package com.tgb.cp_dns.controller.client.booking;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tgb.cp_dns.dto.common.CouponResponse;
import com.tgb.cp_dns.dto.restaurant.AvailableTableResponse;
import com.tgb.cp_dns.dto.restaurant.BookingDetailResponse;
import com.tgb.cp_dns.dto.restaurant.BookingSearchRequest;
import com.tgb.cp_dns.dto.restaurant.BookingSummaryResponse;
import com.tgb.cp_dns.dto.restaurant.CreateBookingRequest;
import com.tgb.cp_dns.dto.restaurant.PaymentPreviewRequest;
import com.tgb.cp_dns.dto.restaurant.PaymentPreviewResponse;
import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.service.ClientBookingService;
import com.tgb.cp_dns.service.PaymentService;
import com.tgb.cp_dns.service.SettingsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class ClientBookingController {

    private final ClientBookingService bookingService;
    private final PaymentService paymentService;
    private final SettingsService settingsService;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    @GetMapping("/operating-hours")
    public ResponseEntity<?> getOperatingHours() {
        return ResponseEntity.ok(Map.of(
                "openingTime", settingsService.getRestaurantOpeningTime().toString(),
                "closingTime", settingsService.getRestaurantClosingTime().toString()));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchTables(@RequestBody @Valid BookingSearchRequest request) {
        return ResponseEntity.ok(bookingService.searchAvailableTables(request));
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<AvailableTableResponse> getTableDetail(@PathVariable Long id) {
        AvailableTableResponse response = bookingService.getTableDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingSummaryResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getMyBookings(status, page, size));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @RequestBody @Valid CreateBookingRequest request,
            HttpServletRequest httpReq) {

        String ipAddress = httpReq.getRemoteAddr();

        return ResponseEntity.ok(bookingService.createBooking(request, ipAddress));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            RestaurantBooking booking = (RestaurantBooking) paymentService.processVnPayCallback(params);

            String accessToken = booking.getAccessToken();
            String txnCode = params.get("vnp_TxnRef");
            String frontendUrl;

            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                frontendUrl = frontendBaseUrl + "/restaurant/booking/success"
                        + "?code=" + txnCode
                        + "&accessToken=" + accessToken;
            } else {
                frontendUrl = frontendBaseUrl + "/restaurant/booking/failed"
                        + "?error=" + URLEncoder.encode("Thanh toán thất bại hoặc bị hủy", StandardCharsets.UTF_8);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl))
                    .build();

        } catch (Exception e) {
            String failedUrl = frontendBaseUrl + "/restaurant/booking/failed?error="
                    + URLEncoder.encode("Lỗi hệ thống", StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(failedUrl)).build();
        }
    }

    @GetMapping("/menu")
    public ResponseEntity<?> getMenuForBooking(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("date") LocalDate date) {
        return ResponseEntity.ok(bookingService.getMenuForBooking(keyword, date));
    }

    @PostMapping("/cancel/{accessToken}")
    public ResponseEntity<?> cancelBooking(@PathVariable String accessToken) {
        bookingService.cancelBooking(accessToken);
        return ResponseEntity.ok("Hủy bàn thành công. Email xác nhận đã được gửi.");
    }

    @PostMapping("/payment-preview")
    public ResponseEntity<PaymentPreviewResponse> previewDeposit(@RequestBody PaymentPreviewRequest request) {
        return ResponseEntity.ok(bookingService.previewDepositPolicy(request));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons() {
        return ResponseEntity.ok(bookingService.getAvailableCoupons());
    }

    @GetMapping("/view/{accessToken}")
    public ResponseEntity<BookingDetailResponse> getBookingDetail(@PathVariable String accessToken) {
        return ResponseEntity.ok(bookingService.getBookingDetail(accessToken));
    }
}
