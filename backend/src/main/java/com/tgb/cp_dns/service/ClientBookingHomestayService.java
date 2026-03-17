package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.homestay.HomestaySearchRequest;
import com.tgb.cp_dns.dto.homestay.RoomTypeDetailResponse;
import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.dto.payment.VNPayRefundResponse;
import com.tgb.cp_dns.dto.restaurant.BookingCreationResponse;
import com.tgb.cp_dns.dto.common.CouponResponse;
import com.tgb.cp_dns.dto.homestay.AvailableRoomTypeResponse;
import com.tgb.cp_dns.dto.homestay.CreateHomestayBookingRequest;
import com.tgb.cp_dns.dto.homestay.HomestayBookingDetailResponse;
import com.tgb.cp_dns.dto.homestay.HomestayBookingSummaryResponse;
import com.tgb.cp_dns.entity.homestay.Room;
import com.tgb.cp_dns.entity.homestay.RoomType;
import com.tgb.cp_dns.entity.homestay.RoomTypeImage;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.entity.common.OutboxMessage;
import com.tgb.cp_dns.entity.common.UserCouponUsage;
import com.tgb.cp_dns.entity.homestay.HomestayAmenity;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.CouponStatus;
import com.tgb.cp_dns.enums.DepositType;
import com.tgb.cp_dns.enums.HomestayCommonStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.enums.RoomStatus;
import com.tgb.cp_dns.enums.ServiceType;
import com.tgb.cp_dns.repository.auth.UserRepository;
import com.tgb.cp_dns.repository.common.CouponRepository;
import com.tgb.cp_dns.repository.common.OutboxMessageRepository;
import com.tgb.cp_dns.repository.common.UserCouponUsageRepository;
import com.tgb.cp_dns.repository.homestay.HomestayBookingRepository;
import com.tgb.cp_dns.repository.homestay.RoomRepository;
import com.tgb.cp_dns.repository.homestay.RoomTypeRepository;
import com.tgb.cp_dns.security.SecurityUser;
import com.tgb.cp_dns.service.queue.OutboxNotifier;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientBookingHomestayService {

        private final HomestayBookingRepository bookingRepository;
        private final RoomRepository roomRepository;
        private final RoomTypeRepository roomTypeRepository;
        private final CouponRepository couponRepository;
        private final UserRepository userRepository;
        private final PaymentService paymentService;
        private final UserCouponUsageRepository usageRepository;
        private final OutboxMessageRepository outboxMessageRepository;
        private final OutboxNotifier outboxNotifier;
        private final EmailService emailService;
        private final SettingsService settingsService;

        @Transactional(readOnly = true)
        public List<AvailableRoomTypeResponse> searchAvailableRoomTypes(HomestaySearchRequest request) {
                if (request.getCheckInDate().isAfter(request.getCheckOutDate()) ||
                                request.getCheckInDate().isEqual(request.getCheckOutDate())) {
                        throw new RuntimeException("Ngày Check-in phải trước ngày Check-out ít nhất 1 đêm");
                }

                int adults = request.getNumberOfAdults() != null ? request.getNumberOfAdults() : 1;
                int children = request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 0;
                double requiredCapacity = adults + (children / 2.0);

                List<Long> occupiedRoomIds = bookingRepository.findOccupiedRoomIds(
                                request.getCheckInDate(),
                                request.getCheckOutDate());

                List<RoomType> activeRoomTypes = roomTypeRepository
                                .findAllByStatusAndIsDeletedFalse(HomestayCommonStatus.ACTIVE);

                List<Room> allActiveRooms = roomRepository.findAllByIsDeletedFalse();

                List<AvailableRoomTypeResponse> results = new ArrayList<>();

                for (RoomType type : activeRoomTypes) {
                        double typeCapacity = type.getMaxAdults() + (type.getMaxChildren() / 2.0);

                        if (typeCapacity < requiredCapacity) {
                                continue;
                        }

                        long availableCount = allActiveRooms.stream()
                                        .filter(room -> room.getRoomType().getTypeId().equals(type.getTypeId()))
                                        .filter(room -> !occupiedRoomIds.contains(room.getRoomId()))
                                        .filter(room -> room.getStatus() != RoomStatus.UNAVAILABLE)
                                        .count();

                        if (availableCount > 0) {
                                results.add(mapToResponse(type, (int) availableCount));
                        }
                }

                results.sort(Comparator.comparing(AvailableRoomTypeResponse::getBasePrice));
                return results;
        }

        private AvailableRoomTypeResponse mapToResponse(RoomType type, int availableCount) {
                String mainImage = type.getImages().stream()
                                .min(Comparator.comparing(RoomTypeImage::getDisplayOrder))
                                .map(RoomTypeImage::getImageUrl)
                                .orElse(null);

                List<String> amenityNames = type.getAmenities().stream()
                                .map(HomestayAmenity::getName)
                                .collect(Collectors.toList());

                return AvailableRoomTypeResponse.builder()
                                .typeId(type.getTypeId())
                                .typeName(type.getName())
                                .className(type.getRoomClass() != null ? type.getRoomClass().getName() : "")
                                .description(type.getDescription())
                                .basePrice(type.getBasePrice())
                                .maxAdults(type.getMaxAdults())
                                .maxChildren(type.getMaxChildren())
                                .availableRoomsCount(availableCount)
                                .mainImage(mainImage)
                                .amenities(amenityNames)
                                .build();
        }

        @Transactional(readOnly = true)
        public RoomTypeDetailResponse getRoomTypeDetail(Long typeId, LocalDate checkIn, LocalDate checkOut) {
                RoomType type = roomTypeRepository.findById(typeId)
                                .filter(t -> !t.getIsDeleted() && t.getStatus() == HomestayCommonStatus.ACTIVE)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Loại phòng không tồn tại hoặc ngừng kinh doanh"));

                List<String> images = type.getImages().stream()
                                .sorted(Comparator.comparing(RoomTypeImage::getDisplayOrder))
                                .map(RoomTypeImage::getImageUrl)
                                .collect(Collectors.toList());

                List<String> amenities = type.getAmenities().stream()
                                .sorted(Comparator.comparing(HomestayAmenity::getAmenityId))
                                .map(HomestayAmenity::getName)
                                .collect(Collectors.toList());

                Integer availableCount = null;
                if (checkIn != null && checkOut != null) {
                        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Ngày Check-in phải trước ngày Check-out");
                        }

                        List<Long> occupiedRoomIds = bookingRepository.findOccupiedRoomIds(checkIn, checkOut);

                        List<Room> physicalRooms = roomRepository.findAllByRoomType_TypeIdAndStatusAndIsDeletedFalse(
                                        typeId,
                                        RoomStatus.AVAILABLE);

                        long count = physicalRooms.stream()
                                        .filter(room -> !occupiedRoomIds.contains(room.getRoomId()))
                                        .count();
                        availableCount = (int) count;
                }

                return RoomTypeDetailResponse.builder()
                                .typeId(type.getTypeId())
                                .typeName(type.getName())
                                .className(type.getRoomClass() != null ? type.getRoomClass().getName() : "Phổ thông")
                                .description(type.getDescription())
                                .basePrice(type.getBasePrice())
                                .maxAdults(type.getMaxAdults())
                                .maxChildren(type.getMaxChildren())
                                .images(images)
                                .amenities(amenities)
                                .availableRoomsCount(availableCount)
                                .build();
        }

        @Transactional
        public BookingCreationResponse createHomestayBooking(CreateHomestayBookingRequest request, String ipAddress) {
                User currentUser = null;
                boolean isStaffAction = false;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                        isStaffAction = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
                        if (!isStaffAction) {
                                currentUser = userRepository.findByPhone(auth.getName()).orElse(null);
                        }
                }

                List<Long> occupiedIds = bookingRepository.findOccupiedRoomIds(request.getCheckInDate(),
                                request.getCheckOutDate());
                List<Room> availableRooms = roomRepository.findAllByRoomType_TypeIdAndStatusAndIsDeletedFalse(
                                request.getRoomTypeId(), RoomStatus.AVAILABLE);

                Room assignedRoom = availableRooms.stream()
                                .filter(r -> !occupiedIds.contains(r.getRoomId()))
                                .findFirst()
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                                "Phòng đã bị đặt trong thời gian này."));

                RoomType roomType = assignedRoom.getRoomType();

                HomestayBooking booking = new HomestayBooking();
                booking.setUser(currentUser);
                booking.setRoom(assignedRoom);
                booking.setCustomerName(request.getCustomerName());
                booking.setCustomerPhone(request.getCustomerPhone());
                booking.setCustomerEmail(request.getCustomerEmail());
                booking.setCheckInDate(request.getCheckInDate());
                booking.setCheckOutDate(request.getCheckOutDate());
                booking.setPaymentMethod(request.getPaymentMethod());
                booking.setStatus(BookingStatus.PENDING);
                booking.setNumberOfAdults(request.getNumberOfAdults());
                booking.setNumberOfChildren(request.getNumberOfChildren());

                booking.setRoomNameSnapshot(roomType.getName());
                booking.setRoomClassNameSnapshot(roomType.getRoomClass().getName());
                booking.setPricePerNightSnapshot(roomType.getBasePrice());

                if (roomType.getImages() != null && !roomType.getImages().isEmpty()) {
                        String mainImageUrl = roomType.getImages().stream()
                                        .min(Comparator.comparing(RoomTypeImage::getDisplayOrder))
                                        .map(RoomTypeImage::getImageUrl)
                                        .orElse(null);
                        booking.setRoomImageSnapshot(mainImageUrl);
                }

                long nights = java.time.temporal.ChronoUnit.DAYS.between(request.getCheckInDate(),
                                request.getCheckOutDate());
                BigDecimal subTotal = roomType.getBasePrice().multiply(BigDecimal.valueOf(nights));
                BigDecimal discountAmount = BigDecimal.ZERO;

                if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
                        String codeInput = request.getCouponCode().trim();
                        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsDeletedFalse(codeInput)
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Mã giảm giá không tồn tại"));

                        if (coupon.getStatus() == CouponStatus.UNAVAILABLE)
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã tạm ngưng.");
                        if (coupon.getServiceType() != ServiceType.HOMESTAY)
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Mã không áp dụng cho Homestay.");

                        LocalDateTime now = LocalDateTime.now();
                        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil()))
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã hết hạn.");
                        if (coupon.getQuantity() != null && coupon.getUsedCount() >= coupon.getQuantity())
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã hết lượt.");
                        if (coupon.isRequireAccount() && currentUser == null)
                                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập.");
                        if (coupon.getMinOrderValue() != null && subTotal.compareTo(coupon.getMinOrderValue()) < 0)
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Đơn chưa đủ giá trị tối thiểu.");

                        if (coupon.getDiscountPercent() != null) {
                                discountAmount = subTotal.multiply(BigDecimal.valueOf(coupon.getDiscountPercent())
                                                .divide(BigDecimal.valueOf(100)));
                                if (coupon.getMaxDiscountAmount() != null
                                                && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                                        discountAmount = coupon.getMaxDiscountAmount();
                                }
                        } else {
                                discountAmount = coupon.getDiscountAmount();
                        }

                        coupon.setUsedCount(coupon.getUsedCount() + 1);
                        couponRepository.save(coupon);

                        if (currentUser != null) {
                                UserCouponUsage usage = new UserCouponUsage();
                                usage.setUser(currentUser);
                                usage.setCoupon(coupon);
                                usage.setUsedAt(LocalDateTime.now());
                                usageRepository.save(usage);
                        }

                        booking.setAppliedCouponCode(coupon.getCode());
                }

                BigDecimal finalTotal = subTotal.subtract(discountAmount);
                if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
                        finalTotal = BigDecimal.ZERO;
                booking.setSubTotal(subTotal);
                booking.setDiscountAmount(discountAmount);
                booking.setTotalAmount(finalTotal);

                BigDecimal depositAmount = finalTotal.multiply(
                                request.getDepositType() == DepositType.FULL_100 ? BigDecimal.ONE
                                                : new BigDecimal("0.5"));
                booking.setDepositAmount(depositAmount);

                boolean isVnpayRequired = depositAmount.compareTo(BigDecimal.ZERO) > 0
                                && request.getPaymentMethod() == PaymentMethod.VNPAY;
                if (isVnpayRequired) {
                        booking.setVnpTxnRef(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
                }

                HomestayBooking savedBooking = bookingRepository.save(booking);

                if (isVnpayRequired) {
                        OutboxMessage outbox = new OutboxMessage();
                        outbox.setEventType("CANCEL_ORDER");
                        outbox.setPayload(savedBooking.getVnpTxnRef());
                        outbox.setSent(false);
                        outbox.setCreatedAt(LocalDateTime.now());
                        outboxMessageRepository.save(outbox);
                        outboxNotifier.notifyNewOutboxMessage();
                }
                String paymentUrl = null;
                String message = "";

                if (isVnpayRequired) {
                        paymentUrl = paymentService.createVnPayPaymentUrl(savedBooking, ipAddress);
                }

                if (isStaffAction) {
                        if (paymentUrl != null) {
                                emailService.sendPaymentRequestEmail(savedBooking.getCustomerEmail(), savedBooking,
                                                paymentUrl);
                                message = "Đã gửi yêu cầu thanh toán qua email khách hàng.";
                                paymentUrl = null;
                        } else {
                                savedBooking.setStatus(BookingStatus.CONFIRMED);
                                bookingRepository.save(savedBooking);
                                emailService.sendBookingSuccessEmail(savedBooking.getCustomerEmail(), savedBooking);
                                message = "Đặt phòng thành công.";
                        }
                } else {
                        emailService.sendBookingCreatedEmail(savedBooking.getCustomerEmail(), savedBooking);
                        message = isVnpayRequired ? "Vui lòng thanh toán cọc để hoàn tất." : "Đặt phòng thành công!";
                }

                return BookingCreationResponse.builder()
                                .bookingCode(savedBooking.getAccessToken())
                                .message(message)
                                .paymentUrl(paymentUrl)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<CouponResponse> getAvailableHomestayCoupons() {
                List<Coupon> coupons = couponRepository.findAvailableCoupons(
                                ServiceType.HOMESTAY,
                                LocalDateTime.now());

                return coupons.stream()
                                .map(this::mapToCouponResponse)
                                .collect(Collectors.toList());
        }

        private CouponResponse mapToCouponResponse(Coupon coupon) {
                return CouponResponse.builder()
                                .couponId(coupon.getCouponId())
                                .code(coupon.getCode())
                                .discountPercent(coupon.getDiscountPercent())
                                .discountAmount(coupon.getDiscountAmount())
                                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                                .minOrderValue(coupon.getMinOrderValue())
                                .validUntil(coupon.getValidUntil())
                                .isRequireAccount(coupon.isRequireAccount())
                                .build();
        }

        @Transactional(readOnly = true)
        public Page<HomestayBookingSummaryResponse> getMyHomestayBookings(BookingStatus status, int page, int size) {
                User currentUser = getAuthenticatedUser();
                Pageable pageable = PageRequest.of(page, size);
                Page<HomestayBooking> bookingPage;

                if (status == null) {
                        bookingPage = bookingRepository
                                        .findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
                } else {
                        bookingPage = bookingRepository.findByUser_UserIdAndStatusOrderByCreatedAtDesc(
                                        currentUser.getUserId(), status, pageable);
                }

                return bookingPage.map(this::mapToHomestaySummaryResponse);
        }

        private HomestayBookingSummaryResponse mapToHomestaySummaryResponse(HomestayBooking booking) {
                return HomestayBookingSummaryResponse.builder()
                                .bookingId(booking.getBookingId())
                                .accessToken(booking.getAccessToken())
                                .checkInDate(booking.getCheckInDate())
                                .checkOutDate(booking.getCheckOutDate())
                                .createdAt(booking.getCreatedAt())
                                .status(booking.getStatus().name())
                                .roomName(booking.getRoomNameSnapshot())
                                .roomImage(booking.getRoomImageSnapshot())
                                .totalAmount(booking.getTotalAmount() != null
                                                ? booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .depositAmount(booking.getDepositAmount() != null
                                                ? booking.getDepositAmount().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .build();
        }

        @Transactional(readOnly = true)
        public HomestayBookingDetailResponse getHomestayBookingDetail(String accessToken) {
                HomestayBooking booking = bookingRepository.findByAccessToken(accessToken)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Đơn đặt phòng không tồn tại hoặc đường dẫn không hợp lệ."));

                if (booking.getUser() != null) {
                        User currentUser;
                        try {
                                currentUser = getAuthenticatedUser();
                        } catch (ResponseStatusException e) {
                                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Vui lòng đăng nhập để xem thông tin đặt phòng này.");
                        }

                        if (!currentUser.getUserId().equals(booking.getUser().getUserId())) {
                                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Bạn không có quyền truy cập thông tin đặt phòng này.");
                        }
                }

                Integer cancelHours = settingsService.getHomestayCancellationDeadline();

                return mapToHomestayBookingDetailResponse(booking, cancelHours);
        }

        private HomestayBookingDetailResponse mapToHomestayBookingDetailResponse(HomestayBooking booking, Integer cancellationNoticeHours) {
                return HomestayBookingDetailResponse.builder()
                                .bookingId(booking.getBookingId())
                                .accessToken(booking.getAccessToken())
                                .status(booking.getStatus())
                                .createdAt(booking.getCreatedAt())
                                .customerName(booking.getCustomerName())
                                .customerPhone(booking.getCustomerPhone())
                                .customerEmail(booking.getCustomerEmail())
                                .roomName(booking.getRoomNameSnapshot())
                                .roomClass(booking.getRoomClassNameSnapshot())
                                .roomImage(booking.getRoomImageSnapshot())
                                .pricePerNight(booking.getPricePerNightSnapshot())
                                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber()
                                                : "Chưa xếp phòng")
                                .checkInDate(booking.getCheckInDate())
                                .checkOutDate(booking.getCheckOutDate())
                                .numberOfAdults(booking.getNumberOfAdults())
                                .numberOfChildren(booking.getNumberOfChildren())
                                .paymentMethod(booking.getPaymentMethod())
                                .paymentTime(booking.getPaymentTime())
                                .subTotal(booking.getSubTotal() != null
                                                ? booking.getSubTotal().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)

                                .discountAmount(booking.getDiscountAmount() != null
                                                ? booking.getDiscountAmount().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)

                                .totalAmount(booking.getTotalAmount() != null
                                                ? booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)

                                .depositAmount(booking.getDepositAmount() != null
                                                ? booking.getDepositAmount().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)

                                .pricePerNight(booking.getPricePerNightSnapshot() != null
                                                ? booking.getPricePerNightSnapshot().setScale(0, RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .appliedCouponCode(booking.getAppliedCouponCode())
                                .vnpTxnRef(booking.getVnpTxnRef())
                                .cancellationNoticeHours(cancellationNoticeHours)
                                .build();
        }

        private User getAuthenticatedUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                        "Vui lòng đăng nhập để thực hiện chức năng này");
                }

                Object principal = auth.getPrincipal();

                if (principal instanceof SecurityUser) {
                        return ((SecurityUser) principal).getUser();
                }

                String username = auth.getName();
                return userRepository.findByPhone(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "Không tìm thấy thông tin người dùng"));
        }

        @Transactional
        public void cancelHomestayBooking(String accessToken) {
                HomestayBooking booking = bookingRepository.findByAccessToken(accessToken)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Đơn đặt phòng không tồn tại hoặc đường dẫn không hợp lệ."));

                if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Trạng thái đơn hàng hiện tại không thể hủy.");
                }

                LocalDateTime checkInDateTime = booking.getCheckInDate().atTime(14, 0);
                LocalDateTime now = LocalDateTime.now();
                int homestayDeadlineHours = settingsService.getHomestayCancellationDeadline();
                LocalDateTime deadline = checkInDateTime.minusHours(homestayDeadlineHours);

                if (now.isAfter(deadline)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Quý khách chỉ có thể hủy đơn đặt phòng miễn phí trước giờ nhận phòng 48 tiếng. "
                                                        +
                                                        "Vui lòng liên hệ hotline để được hỗ trợ.");
                }

                if (booking.getPaymentMethod() == PaymentMethod.VNPAY) {
                        boolean shouldRefund = false;

                        if (booking.getStatus() == BookingStatus.CONFIRMED) {
                                shouldRefund = true;
                        } else if (booking.getStatus() == BookingStatus.PENDING) {
                                try {
                                        String txnDate = booking.getCreatedAt()
                                                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                                        VNPayQueryResponse queryRes = paymentService
                                                        .queryTransaction(booking.getVnpTxnRef(), txnDate);

                                        if ("00".equals(queryRes.getResponseCode())
                                                        && "00".equals(queryRes.getTransactionStatus())) {
                                                shouldRefund = true;
                                                System.out.println(
                                                                "Phát hiện đơn PENDING nhưng thực tế đã thanh toán. Sẽ hoàn tiền.");
                                        }
                                } catch (Exception e) {
                                        System.err.println("Lỗi truy vấn VNPay khi hủy đơn Homestay PENDING: "
                                                        + e.getMessage());
                                }
                        }

                        if (shouldRefund) {
                                try {
                                        LocalDateTime refundBaseTime = (booking.getPaymentTime() != null)
                                                        ? booking.getPaymentTime()
                                                        : LocalDateTime.now();

                                        String transactionDate = refundBaseTime
                                                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

                                        VNPayRefundResponse refundRes = paymentService.refundTransaction(
                                                        booking.getVnpTxnRef(),
                                                        booking.getDepositAmount(),
                                                        transactionDate,
                                                        booking.getCustomerName() != null ? booking.getCustomerName()
                                                                        : "Khach hang Homestay");

                                        if ("00".equals(refundRes.getVnp_ResponseCode())) {
                                                System.out.println("Hoàn tiền VNPay thành công cho đơn phòng: "
                                                                + booking.getBookingId());
                                        } else {
                                                System.err.println("Hoàn tiền VNPay thất bại: "
                                                                + refundRes.getVnp_Message());
                                        }
                                } catch (Exception e) {
                                        System.err.println("Lỗi kết nối VNPay Refund khi hủy đơn Homestay: "
                                                        + e.getMessage());
                                }
                        }
                }

                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                try {
                        emailService.sendBookingCancellationEmail(booking.getCustomerEmail(), booking);
                } catch (Exception e) {
                        System.err.println("Lỗi gửi email hủy phòng: " + e.getMessage());
                }
        }
}