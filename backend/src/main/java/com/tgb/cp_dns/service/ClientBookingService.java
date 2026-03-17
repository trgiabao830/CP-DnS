package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.common.CouponResponse;
import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.dto.payment.VNPayRefundResponse;
import com.tgb.cp_dns.dto.restaurant.*;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.entity.common.OutboxMessage;
import com.tgb.cp_dns.entity.common.UserCouponUsage;
import com.tgb.cp_dns.entity.restaurant.*;
import com.tgb.cp_dns.enums.*;
import com.tgb.cp_dns.repository.restaurant.*;
import com.tgb.cp_dns.security.SecurityUser;
import com.tgb.cp_dns.service.queue.OutboxNotifier;
import com.tgb.cp_dns.repository.auth.UserRepository;
import com.tgb.cp_dns.repository.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientBookingService {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantBookingRepository bookingRepository;
    private final FoodVariantOptionRepository optionRepository;
    private final PaymentService paymentService;
    private final FoodRepository foodRepository;
    private final EmailService emailService;
    private final CouponRepository couponRepository;
    private final SystemConfigRepository configRepository;
    private final UserRepository userRepository;
    private final UserCouponUsageRepository usageRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxNotifier outboxNotifier;
    private final SettingsService settingsService;

    private static final int DINING_DURATION_HOURS = 2;
    private static final BigDecimal FIXED_DEPOSIT_AMOUNT = new BigDecimal("100000");

    @Transactional(readOnly = true)
    public List<AvailableTableResponse> searchAvailableTables(BookingSearchRequest request) {
        LocalDateTime startTime = LocalDateTime.of(request.getDate(), request.getTime());
        LocalDateTime endTime = startTime.plusHours(DINING_DURATION_HOURS);

        List<Long> occupiedTableIds = tableRepository.findOccupiedTableIds(startTime, endTime);

        int guests = request.getNumberOfGuests();
        int maxCapacity = guests + 2;

        List<RestaurantTable> candidateTables = tableRepository
                .findAllByCapacityBetweenAndStatusAndIsDeletedFalseOrderByTableIdAsc(
                        guests, maxCapacity, TableStatus.AVAILABLE);

        Map<String, List<RestaurantTable>> grouped = candidateTables.stream()
                .filter(t -> !occupiedTableIds.contains(t.getTableId()))
                .collect(Collectors.groupingBy(
                        t -> t.getArea().getName() + " - Bàn " + t.getCapacity() + " người"));

        List<AvailableTableResponse> responses = new ArrayList<>();

        for (Map.Entry<String, List<RestaurantTable>> entry : grouped.entrySet()) {
            List<RestaurantTable> tables = entry.getValue();
            if (tables.isEmpty())
                continue;

            RestaurantTable suggestedTable = tables.get(0);

            responses.add(AvailableTableResponse.builder()
                    .areaId(suggestedTable.getArea().getAreaId())
                    .areaName(suggestedTable.getArea().getName())
                    .capacity(suggestedTable.getCapacity())
                    .suggestedTableId(suggestedTable.getTableId())
                    .tableName(suggestedTable.getTableNumber())
                    .remainingTables(tables.size())
                    .build());
        }

        responses.sort(Comparator.comparing(AvailableTableResponse::getAreaName));
        return responses;
    }

    @Transactional(readOnly = true)
    public AvailableTableResponse getTableDetail(Long tableId) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Bàn không tồn tại hoặc đã bị xóa (ID: " + tableId + ")"));

        return AvailableTableResponse.builder()
                .areaId(table.getArea().getAreaId())
                .areaName(table.getArea().getName())
                .capacity(table.getCapacity())
                .suggestedTableId(table.getTableId())
                .tableName(table.getTableNumber())
                .remainingTables(1)
                .build();
    }

    @Transactional
    public BookingCreationResponse createBooking(CreateBookingRequest request, String ipAddress) {

        LocalTime requestedTime = request.getTime();
        LocalTime openingTime = settingsService.getRestaurantOpeningTime();
        LocalTime closingTime = settingsService.getRestaurantClosingTime();

        if (requestedTime.isBefore(openingTime) || requestedTime.isAfter(closingTime)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String msg = String.format("Nhà hàng chỉ nhận đặt bàn trong khoảng từ %s đến %s. Vui lòng chọn giờ khác.",
                    openingTime.format(formatter), closingTime.format(formatter));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }

        User currentUser = null;
        boolean isStaffAction = false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            isStaffAction = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

            if (!isStaffAction) {
                String phone = auth.getName();
                currentUser = userRepository.findByPhone(phone).orElse(null);
            }
        }
        LocalDateTime startTime = LocalDateTime.of(request.getDate(), request.getTime());
        LocalDateTime endTime = startTime.plusHours(DINING_DURATION_HOURS);

        List<Long> occupiedIds = tableRepository.findOccupiedTableIds(startTime, endTime);
        if (occupiedIds.contains(request.getTableId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn này vừa có người đặt, vui lòng chọn bàn khác.");
        }

        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn không tồn tại"));

        RestaurantBooking booking = new RestaurantBooking();

        booking.setUser(currentUser);

        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setCustomerEmail(request.getCustomerEmail());
        booking.setTable(table);
        booking.setBookingTime(startTime);
        booking.setEndTime(endTime);
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setPaymentMethod(request.getPaymentMethod());

        if (Boolean.TRUE.equals(request.getIsPreOrderFood())) {
            booking.setBookingType(BookingType.ONLINE_PREORDER);
        } else {
            booking.setBookingType(BookingType.ONLINE_TABLEONLY);
        }

        BigDecimal totalFoodAmount = BigDecimal.ZERO;
        List<RestaurantOrderDetail> orderDetails = new ArrayList<>();

        if (Boolean.TRUE.equals(request.getIsPreOrderFood()) && request.getOrderItems() != null) {
            boolean isToday = request.getDate().isEqual(LocalDate.now());

            for (OrderItemRequest itemReq : request.getOrderItems()) {
                RestaurantOrderDetail detail = createOrderDetail(booking, itemReq, isToday);

                orderDetails.add(detail);
                totalFoodAmount = totalFoodAmount.add(detail.getTotalPrice());
            }
        }
        booking.setOrderDetails(orderDetails);

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            String codeInput = request.getCouponCode().trim();

            Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsDeletedFalse(codeInput)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Mã giảm giá '" + codeInput + "' không tồn tại"));

            if (coupon.getStatus() == CouponStatus.UNAVAILABLE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã giảm giá này hiện đang tạm ngưng sử dụng.");
            }

            if (coupon.getServiceType() != ServiceType.RESTAURANT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã giảm giá này không áp dụng cho đặt bàn nhà hàng.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá chưa đến đợt sử dụng.");
            }

            if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn.");
            }

            if (coupon.getQuantity() != null && coupon.getUsedCount() >= coupon.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng.");
            }
            if (coupon.isRequireAccount() && currentUser == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Mã giảm giá này chỉ dành cho thành viên đã đăng nhập.");
            }

            if (coupon.getMinOrderValue() != null && totalFoodAmount.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn chưa đủ giá trị tối thiểu.");
            }

            if (coupon.getDiscountPercent() != null) {
                BigDecimal percent = BigDecimal.valueOf(coupon.getDiscountPercent()).divide(BigDecimal.valueOf(100));
                discountAmount = totalFoodAmount.multiply(percent);
                if (coupon.getMaxDiscountAmount() != null
                        && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
            } else if (coupon.getDiscountAmount() != null) {
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

        BigDecimal finalTotalAmount = totalFoodAmount.subtract(discountAmount);
        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0)
            finalTotalAmount = BigDecimal.ZERO;

        booking.setSubTotal(totalFoodAmount);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(finalTotalAmount);

        BigDecimal depositAmount = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getIsPreOrderFood()) && finalTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getDepositType() == DepositType.FULL_100) {
                depositAmount = finalTotalAmount;
            } else {
                depositAmount = finalTotalAmount.multiply(new BigDecimal("0.5"));
            }
        } else {
            if (isDepositRequiredNoFood()) {
                depositAmount = FIXED_DEPOSIT_AMOUNT;
            } else {
                depositAmount = BigDecimal.ZERO;
            }
        }

        booking.setDepositAmount(depositAmount);

        boolean isVnpayRequired = (depositAmount.compareTo(BigDecimal.ZERO) > 0)
                && (request.getPaymentMethod() == PaymentMethod.VNPAY);

        booking.setStatus(BookingStatus.PENDING);

        if (isVnpayRequired) {
            String vnpTxnRef = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            booking.setVnpTxnRef(vnpTxnRef);
        } else {
            booking.setVnpTxnRef(null);
        }

        RestaurantBooking savedBooking = bookingRepository.save(booking);

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

        if (booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                && request.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentUrl = paymentService.createVnPayPaymentUrl(savedBooking, ipAddress);
        }

        if (isStaffAction) {
            if (paymentUrl != null) {
                try {
                    emailService.sendPaymentRequestEmail(savedBooking.getCustomerEmail(), savedBooking, paymentUrl);
                    message = "Đã tạo đơn thành công. Link thanh toán đã gửi email khách.";
                    paymentUrl = null;
                } catch (Exception e) {
                }
            } else {
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                try {
                    emailService.sendBookingSuccessEmail(savedBooking.getCustomerEmail(), savedBooking);
                    message = "Đặt bàn thành công (Không cọc). Email xác nhận đã gửi.";
                } catch (Exception e) {
                }
            }
        } else {
            try {
                emailService.sendBookingCreatedEmail(savedBooking.getCustomerEmail(), savedBooking);
            } catch (Exception e) {
            }

            if (paymentUrl != null) {
                message = "Vui lòng thanh toán " + booking.getDepositAmount() + " để hoàn tất.";
            } else {
                message = "Đặt bàn thành công, vui lòng đến đúng giờ.";
            }
        }

        return BookingCreationResponse.builder()
                .bookingCode(savedBooking.getAccessToken())
                .message(message)
                .paymentUrl(paymentUrl)
                .build();
    }

    private RestaurantOrderDetail createOrderDetail(RestaurantBooking booking, OrderItemRequest itemReq,
            boolean checkStock) {
        Food food = foodRepository.findById(itemReq.getFoodId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Món ăn ID " + itemReq.getFoodId() + " không tồn tại"));

        if (Boolean.TRUE.equals(food.getIsDeleted()) || food.getStatus() == FoodStatus.UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Món '" + food.getName() + "' đã ngừng kinh doanh, vui lòng bỏ chọn.");
        }

        if (checkStock && food.getStatus() == FoodStatus.OUT_OF_STOCK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Món '" + food.getName() + "' đang tạm hết hàng hôm nay.");
        }

        RestaurantOrderDetail detail = new RestaurantOrderDetail();
        detail.setBooking(booking);
        detail.setFood(food);
        detail.setQuantity(itemReq.getQuantity());
        detail.setFoodNameSnapshot(food.getName());
        detail.setFoodImageSnapshot(food.getImageUrl());
        detail.setNote(itemReq.getNote());

        BigDecimal unitPrice = (food.getDiscountPrice() != null) ? food.getDiscountPrice() : food.getBasePrice();
        detail.setUnitPrice(unitPrice);

        BigDecimal itemTotalOptionPrice = BigDecimal.ZERO;
        List<RestaurantOrderOption> selectedOptions = new ArrayList<>();

        if (itemReq.getOptionIds() != null) {
            for (Long optionId : itemReq.getOptionIds()) {
                FoodVariantOption optionMaster = optionRepository.findById(optionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Option ID " + optionId + " không tồn tại"));

                RestaurantOrderOption orderOpt = new RestaurantOrderOption();
                orderOpt.setOrderDetail(detail);
                orderOpt.setOption(optionMaster);
                orderOpt.setVariantNameSnapshot(optionMaster.getVariant().getName());
                orderOpt.setOptionNameSnapshot(optionMaster.getName());
                BigDecimal safeOptionPrice = (optionMaster.getPriceAdjustment() != null)
                        ? optionMaster.getPriceAdjustment()
                        : BigDecimal.ZERO;

                orderOpt.setPriceAdjustmentSnapshot(safeOptionPrice);
                selectedOptions.add(orderOpt);

                itemTotalOptionPrice = itemTotalOptionPrice.add(safeOptionPrice);
            }
        }
        detail.setSelectedOptions(selectedOptions);

        BigDecimal lineTotal = unitPrice.add(itemTotalOptionPrice)
                .multiply(BigDecimal.valueOf(detail.getQuantity()));
        detail.setTotalPrice(lineTotal);

        return detail;
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> getMenuForBooking(String keyword, LocalDate bookingDate) {
        List<FoodStatus> visibleStatuses = List.of(FoodStatus.AVAILABLE, FoodStatus.OUT_OF_STOCK);

        String searchPattern = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchPattern = "%" + keyword.trim().toLowerCase() + "%";
        }

        List<Food> foods = foodRepository.searchFoodsForBooking(searchPattern, visibleStatuses);

        return foods.stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    private FoodResponse mapToFoodResponse(Food food) {
        List<FoodResponse.VariantDto> variantDtos = food.getVariants().stream()
                .filter(v -> !v.getIsDeleted())
                .map(this::mapToVariantDto)
                .filter(vDto -> vDto.getOptions() != null && !vDto.getOptions().isEmpty())
                .collect(Collectors.toList());

        return FoodResponse.builder()
                .foodId(food.getFoodId())
                .name(food.getName())
                .description(food.getDescription())
                .basePrice(food.getBasePrice())
                .discountPrice(food.getDiscountPrice())
                .imageUrl(food.getImageUrl())
                .status(food.getStatus().name())
                .categoryId(food.getCategory() != null ? food.getCategory().getCategoryId() : null)
                .categoryName(food.getCategory() != null ? food.getCategory().getName() : null)
                .variants(variantDtos)
                .build();
    }

    private FoodResponse.VariantDto mapToVariantDto(FoodVariant variant) {
        List<FoodResponse.OptionDto> optionDtos = variant.getOptions().stream()
                .filter(o -> !o.getIsDeleted())
                .map(this::mapToOptionDto)
                .filter(oDto -> !"UNAVAILABLE".equalsIgnoreCase(oDto.getStatus()))
                .sorted(Comparator.comparing(FoodResponse.OptionDto::getOptionId))
                .collect(Collectors.toList());

        return FoodResponse.VariantDto.builder()
                .variantId(variant.getVariantId())
                .name(variant.getName())
                .isRequired(variant.getIsRequired())
                .options(optionDtos)
                .build();
    }

    private FoodResponse.OptionDto mapToOptionDto(FoodVariantOption option) {
        String finalStatus = option.getStatus().name();
        List<FoodResponse.VariantDto> linkedVariants = new ArrayList<>();
        Long linkedCategoryId = null;

        if (option.getLinkedFood() != null) {
            Food linked = option.getLinkedFood();
            if (linked.getIsDeleted() || linked.getStatus() != FoodStatus.AVAILABLE) {
                finalStatus = "UNAVAILABLE";
            } else {
                if (linked.getCategory() != null) {
                    linkedCategoryId = linked.getCategory().getCategoryId();
                }

                linkedVariants = linked.getVariants().stream()
                        .filter(v -> !v.getIsDeleted())
                        .map(this::mapToVariantDto)
                        .sorted(Comparator.comparing(FoodResponse.VariantDto::getVariantId))
                        .collect(Collectors.toList());
            }
        }

        return FoodResponse.OptionDto.builder()
                .optionId(option.getOptionId())
                .name(option.getName())
                .priceAdjustment(option.getPriceAdjustment())
                .status(finalStatus)
                .linkedFoodId(option.getLinkedFood() != null ? option.getLinkedFood().getFoodId() : null)
                .linkedFoodCategoryId(linkedCategoryId)
                .linkedVariants(linkedVariants)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getMyBookings(BookingStatus status, int page, int size) {

        User currentUser = getAuthenticatedUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantBooking> bookingPage;

        if (status == null) {
            bookingPage = bookingRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);
        } else {
            bookingPage = bookingRepository.findByUser_UserIdAndStatusOrderByCreatedAtDesc(currentUser.getUserId(),
                    status, pageable);
        }

        return bookingPage.map(this::mapToSummaryResponse);
    }

    private BookingSummaryResponse mapToSummaryResponse(RestaurantBooking booking) {
        return BookingSummaryResponse.builder()
                .bookingId(booking.getBookingId())
                .accessToken(booking.getAccessToken())
                .bookingTime(booking.getBookingTime())
                .createdAt(booking.getCreatedAt())
                .status(booking.getStatus().name())
                .numberOfGuests(booking.getNumberOfGuests())

                .totalAmount(booking.getTotalAmount() != null
                        ? booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .depositAmount(booking.getDepositAmount() != null
                        ? booking.getDepositAmount().setScale(0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(String accessToken) {
        RestaurantBooking booking = bookingRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Đơn đặt bàn không tồn tại hoặc đường dẫn không hợp lệ."));

        if (booking.getUser() != null) {
            User currentUser;
            try {
                currentUser = getAuthenticatedUser();
            } catch (ResponseStatusException e) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vui lòng đăng nhập để xem đơn hàng thành viên.");
            }

            if (!currentUser.getUserId().equals(booking.getUser().getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn không có quyền xem đơn hàng này.");
            }
        }

        boolean hasDeposit = booking.getDepositAmount() != null
                && booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0;

        int cancelHours = hasDeposit
                ? settingsService.getRestaurantCancellationDeadlineWithDeposit()
                : settingsService.getRestaurantCancellationDeadlineNoDeposit();

        return mapToBookingDetailResponse(booking, cancelHours);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện chức năng này");
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

    private BookingDetailResponse mapToBookingDetailResponse(RestaurantBooking booking, int cancellationNoticeHours) {
        List<BookingDetailResponse.DetailItemDto> items = new ArrayList<>();

        if (booking.getOrderDetails() != null) {
            items = booking.getOrderDetails().stream().map(d -> {
                List<String> optNames = d.getSelectedOptions().stream()
                        .map(o -> o.getOptionNameSnapshot() + " (" + o.getVariantNameSnapshot() + ")")
                        .collect(Collectors.toList());

                return BookingDetailResponse.DetailItemDto.builder()
                        .foodName(d.getFoodNameSnapshot())
                        .foodImage(d.getFoodImageSnapshot())
                        .quantity(d.getQuantity())
                        .note(d.getNote())

                        .unitPrice(d.getUnitPrice() != null ? d.getUnitPrice().setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .totalPrice(d.getTotalPrice() != null ? d.getTotalPrice().setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)

                        .options(optNames)
                        .build();
            }).collect(Collectors.toList());
        }

        return BookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())

                .bookingTime(booking.getBookingTime())
                .createdAt(booking.getCreatedAt())

                .tableNumber(booking.getTable() != null ? booking.getTable().getTableNumber() : "Chưa xếp bàn")
                .numberOfGuests(booking.getNumberOfGuests())

                .subTotal(booking.getSubTotal() != null ? booking.getSubTotal().setScale(0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .discountAmount(
                        booking.getDiscountAmount() != null
                                ? booking.getDiscountAmount().setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                .depositAmount(
                        booking.getDepositAmount() != null
                                ? booking.getDepositAmount().setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                .totalAmount(
                        booking.getTotalAmount() != null ? booking.getTotalAmount().setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)

                .paymentMethod(booking.getPaymentMethod().name())
                .status(booking.getStatus().name())
                .bookingType(booking.getBookingType().name())
                .orderItems(items)
                .cancellationNoticeHours(cancellationNoticeHours)
                .build();
    }

    @Transactional
    public void cancelBooking(String accessToken) {
        RestaurantBooking booking = bookingRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Đơn đặt bàn không tồn tại hoặc đường dẫn không hợp lệ."));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ để hủy.");
        }

        LocalDateTime diningTime = booking.getBookingTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refundTime = booking.getPaymentTime();
        boolean hasDeposit = booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0;

        if (hasDeposit) {
            int depositDeadlineHours = settingsService.getRestaurantCancellationDeadlineWithDeposit();
            LocalDateTime deadline = diningTime.minusHours(depositDeadlineHours);
            if (now.isAfter(deadline)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Do đơn hàng có đặt cọc/đặt món, quý khách chỉ có thể hủy miễn phí trước giờ nhận bàn "
                                + depositDeadlineHours + " tiếng.");
            }
        } else {
            int noDepositDeadlineHours = settingsService.getRestaurantCancellationDeadlineNoDeposit();
            LocalDateTime deadlineNoDeposit = diningTime.minusHours(noDepositDeadlineHours);
            if (now.isAfter(deadlineNoDeposit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn hàng sắp đến giờ nhận bàn (hoặc đã quá giờ), vui lòng liên hệ hotline để được hỗ trợ.");
            }
        }

        if (hasDeposit && booking.getPaymentMethod() == PaymentMethod.VNPAY) {

            boolean shouldRefund = false;

            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                shouldRefund = true;
            } else if (booking.getStatus() == BookingStatus.PENDING) {
                try {
                    String txnDate = booking.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    VNPayQueryResponse queryRes = paymentService.queryTransaction(booking.getVnpTxnRef(), txnDate);

                    if ("00".equals(queryRes.getResponseCode()) && "00".equals(queryRes.getTransactionStatus())) {
                        shouldRefund = true;
                        System.out.println("Phát hiện đơn PENDING nhưng thực tế đã thanh toán. Sẽ hoàn tiền.");
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi truy vấn trạng thái đơn PENDING khi hủy: " + e.getMessage());
                }
            }

            if (shouldRefund) {
                try {
                    String transactionDate = refundTime
                            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    VNPayRefundResponse refundRes = paymentService.refundTransaction(
                            booking.getVnpTxnRef(),
                            booking.getDepositAmount(),
                            transactionDate,
                            booking.getCustomerName() != null ? booking.getCustomerName() : "Unknown Customer");

                    if ("00".equals(refundRes.getVnp_ResponseCode())) {
                        System.out.println("Hoàn tiền thành công cho đơn: " + booking.getBookingId());
                    } else {
                        System.err.println("Hoàn tiền tự động thất bại (VNPay Error): " + refundRes.getVnp_Message());
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi kết nối đến VNPay Refund Service: " + e.getMessage());
                }
            }
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        try {
            emailService.sendBookingCancellationEmail(booking.getCustomerEmail(), booking);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email hủy: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAvailableCoupons() {
        List<Coupon> coupons = couponRepository.findAvailableCoupons(
                ServiceType.RESTAURANT,
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
    public PaymentPreviewResponse previewDepositPolicy(PaymentPreviewRequest request) {

        if (Boolean.TRUE.equals(request.getIsPreOrderFood())) {
            return PaymentPreviewResponse.builder()
                    .isDepositRequired(true)
                    .depositAmount(BigDecimal.ZERO)
                    .build();
        }

        boolean requireDeposit = isDepositRequiredNoFood();

        if (requireDeposit) {
            return PaymentPreviewResponse.builder()
                    .isDepositRequired(true)
                    .depositAmount(FIXED_DEPOSIT_AMOUNT)
                    .build();
        } else {
            return PaymentPreviewResponse.builder()
                    .isDepositRequired(false)
                    .depositAmount(BigDecimal.ZERO)
                    .build();
        }
    }

    private boolean isDepositRequiredNoFood() {
        return configRepository.findByConfigKey("BOOKING_REQUIRE_DEPOSIT_NO_FOOD")
                .map(cfg -> Boolean.parseBoolean(cfg.getConfigValue()))
                .orElse(true);
    }
}
