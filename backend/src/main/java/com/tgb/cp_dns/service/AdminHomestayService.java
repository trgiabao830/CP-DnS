package com.tgb.cp_dns.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.dto.payment.VNPayRefundResponse;
import com.tgb.cp_dns.dto.restaurant.BookingCreationResponse;
import com.tgb.cp_dns.entity.common.Coupon;
import com.tgb.cp_dns.entity.homestay.HomestayAmenity;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.entity.homestay.HomestayRoomClass;
import com.tgb.cp_dns.entity.homestay.Room;
import com.tgb.cp_dns.entity.homestay.RoomType;
import com.tgb.cp_dns.entity.homestay.RoomTypeImage;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.HomestayCommonStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.enums.RoomStatus;
import com.tgb.cp_dns.enums.ServiceType;
import com.tgb.cp_dns.repository.common.CouponRepository;
import com.tgb.cp_dns.repository.homestay.HomestayAmenityRepository;
import com.tgb.cp_dns.repository.homestay.HomestayBookingRepository;
import com.tgb.cp_dns.repository.homestay.HomestayRoomClassRepository;
import com.tgb.cp_dns.repository.homestay.RoomRepository;
import com.tgb.cp_dns.repository.homestay.RoomTypeRepository;
import com.tgb.cp_dns.repository.homestay.RoomTypeImageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHomestayService {

    private final HomestayAmenityRepository amenityRepository;
    private final HomestayRoomClassRepository roomClassRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeImageRepository roomTypeImageRepository;
    private final CloudinaryService cloudinaryService;
    private final SseNotificationService sseService;
    private final HomestayBookingRepository homestayBookingRepository;;
    private final CouponRepository couponRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public Page<HomestayAmenity> getAmenities(String keyword, String statusStr, Pageable pageable) {
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        HomestayCommonStatus status = parseCommonStatus(statusStr);
        return amenityRepository.search(finalKeyword, status, pageable);
    }

    @Transactional
    public HomestayAmenity createAmenity(AmenityRequest req) {
        if (amenityRepository.existsByNameIgnoreCaseAndIsDeletedFalse(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên tiện ích đã tồn tại.");
        }

        HomestayAmenity amenity = new HomestayAmenity();
        amenity.setName(req.getName());
        amenity.setStatus(HomestayCommonStatus.ACTIVE);
        amenity.setIsDeleted(false);
        HomestayAmenity saved = amenityRepository.save(amenity);
        sseService.sendNotification("AMENITY_UPDATE", saved.getAmenityId());
        return saved;
    }

    @Transactional
    public HomestayAmenity updateAmenity(Long id, AmenityRequest req) {
        HomestayAmenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tiện ích không tồn tại"));

        if (amenityRepository.existsByNameIgnoreCaseAndAmenityIdNotAndIsDeletedFalse(req.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên tiện ích đã tồn tại.");
        }

        amenity.setName(req.getName());
        HomestayAmenity saved = amenityRepository.save(amenity);
        sseService.sendNotification("AMENITY_UPDATE", id);
        return saved;
    }

    @Transactional
    public void updateAmenityStatus(Long id, HomestayCommonStatus status) {
        HomestayAmenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tiện ích không tồn tại"));
        amenity.setStatus(status);
        amenityRepository.save(amenity);
        sseService.sendNotification("AMENITY_UPDATE", id);
    }

    @Transactional
    public void deleteAmenity(Long id) {
        HomestayAmenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        amenity.setIsDeleted(true);
        amenityRepository.save(amenity);
        sseService.sendNotification("AMENITY_UPDATE", id);
    }

    @Transactional(readOnly = true)
    public Page<HomestayRoomClass> getRoomClasses(String keyword, String statusStr, Pageable pageable) {
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        HomestayCommonStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = HomestayCommonStatus.valueOf(statusStr);
            } catch (Exception e) {
                status = null;
            }
        }

        return roomClassRepository.search(finalKeyword, status, pageable);
    }

    @Transactional
    public HomestayRoomClass createRoomClass(RoomClassRequest req) {
        if (roomClassRepository.existsByNameIgnoreCaseAndIsDeletedFalse(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hạng phòng đã tồn tại.");
        }

        HomestayRoomClass roomClass = new HomestayRoomClass();
        roomClass.setName(req.getName());
        roomClass.setStatus(HomestayCommonStatus.ACTIVE);
        roomClass.setIsDeleted(false);
        HomestayRoomClass saved = roomClassRepository.save(roomClass);
        sseService.sendNotification("ROOM_CLASS_UPDATE", saved.getClassId());
        return saved;
    }

    @Transactional
    public HomestayRoomClass updateRoomClass(Long id, RoomClassRequest req) {
        HomestayRoomClass roomClass = roomClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (roomClassRepository.existsByNameIgnoreCaseAndClassIdNotAndIsDeletedFalse(req.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hạng phòng đã tồn tại.");
        }

        roomClass.setName(req.getName());
        HomestayRoomClass saved = roomClassRepository.save(roomClass);
        sseService.sendNotification("ROOM_CLASS_UPDATE", id);
        return saved;
    }

    @Transactional
    public void updateRoomClassStatus(Long id, HomestayCommonStatus status) {
        HomestayRoomClass roomClass = roomClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        roomClass.setStatus(status);
        roomClassRepository.save(roomClass);
        sseService.sendNotification("ROOM_CLASS_UPDATE", id);
    }

    @Transactional
    public void deleteRoomClass(Long id) {
        HomestayRoomClass roomClass = roomClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (roomTypeRepository.existsByRoomClass_ClassIdAndIsDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa hạng phòng này vì vẫn còn loại phòng đang hoạt động bên trong.");
        }

        roomClass.setIsDeleted(true);
        roomClassRepository.save(roomClass);
        sseService.sendNotification("ROOM_CLASS_UPDATE", id);
    }

    @Transactional(readOnly = true)
    public Page<RoomType> getRoomTypes(String keyword, String statusStr, Long classId, Pageable pageable) {
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        HomestayCommonStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = HomestayCommonStatus.valueOf(statusStr);
            } catch (Exception e) {
                status = null;
            }
        }

        return roomTypeRepository.search(finalKeyword, status, classId, pageable);
    }

    @Transactional
    public RoomType createRoomType(RoomTypeRequest req, List<MultipartFile> images) {
        HomestayRoomClass roomClass = roomClassRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (roomTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalse(req.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên loại phòng đã tồn tại.");
        }

        RoomType type = new RoomType();
        mapRoomTypeData(type, req, roomClass);
        type.setStatus(HomestayCommonStatus.ACTIVE);
        type.setIsDeleted(false);

        RoomType savedType = roomTypeRepository.save(type);

        if (images != null && !images.isEmpty()) {
            uploadNewImages(savedType, images, 0);
        }

        sseService.sendNotification("ROOM_TYPE_UPDATE", savedType.getTypeId());
        return savedType;
    }

    @Transactional
    public void updateRoomType(Long id, RoomTypeRequest req, List<MultipartFile> newImages) {
        RoomType type = roomTypeRepository.findByTypeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loại phòng không tồn tại"));

        if (roomTypeRepository.existsByNameIgnoreCaseAndTypeIdNotAndIsDeletedFalse(req.getName().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên loại phòng đã tồn tại.");
        }

        HomestayRoomClass roomClass = type.getRoomClass();
        if (req.getClassId() != null && !req.getClassId().equals(roomClass.getClassId())) {
            roomClass = roomClassRepository.findById(req.getClassId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hạng phòng không tồn tại"));
        }

        mapRoomTypeData(type, req, roomClass);

        List<RoomTypeImage> currentDbImages = roomTypeImageRepository.findByRoomType_TypeIdOrderByDisplayOrderAsc(id);

        List<Long> mixedOrderList = (req.getKeptImageIds() == null) ? new ArrayList<>() : req.getKeptImageIds();

        List<Long> idsToKeep = mixedOrderList.stream().filter(val -> val > 0).toList();

        for (RoomTypeImage img : currentDbImages) {
            if (!idsToKeep.contains(img.getImageId())) {
                roomTypeImageRepository.delete(img);
            }
        }

        roomTypeImageRepository.flush();

        int newImageIndexCounter = 0;

        for (int i = 0; i < mixedOrderList.size(); i++) {
            Long itemId = mixedOrderList.get(i);

            if (itemId > 0) {
                Long finalId = itemId;
                RoomTypeImage existingImg = currentDbImages.stream()
                        .filter(img -> img.getImageId().equals(finalId))
                        .findFirst()
                        .orElse(null);

                if (existingImg != null) {
                    if (existingImg.getDisplayOrder() != i) {
                        existingImg.setDisplayOrder(i);
                        roomTypeImageRepository.save(existingImg);
                    }
                }
            } else {
                if (newImages != null && newImageIndexCounter < newImages.size()) {
                    MultipartFile file = newImages.get(newImageIndexCounter);

                    try {
                        String url = cloudinaryService.uploadImage(file, "room-type-" + type.getTypeId(),
                                "homestay_images");

                        RoomTypeImage newImgEntity = new RoomTypeImage();
                        newImgEntity.setRoomType(type);
                        newImgEntity.setImageUrl(url);
                        newImgEntity.setDisplayOrder(i);

                        roomTypeImageRepository.save(newImgEntity);
                    } catch (IOException e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi upload ảnh");
                    }

                    newImageIndexCounter++;
                }
            }
        }

        roomTypeRepository.save(type);
        sseService.sendNotification("ROOM_TYPE_UPDATE", id);
    }

    @Transactional
    public void updateRoomTypeStatus(Long id, HomestayCommonStatus status) {
        RoomType type = roomTypeRepository.findByTypeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        type.setStatus(status);
        roomTypeRepository.save(type);
        sseService.sendNotification("ROOM_TYPE_UPDATE", id);
    }

    private void uploadNewImages(RoomType type, List<MultipartFile> images, int startOrder) {
        List<RoomTypeImage> imageEntities = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            try {
                String url = cloudinaryService.uploadImage(file, "room-type-" + type.getTypeId(), "homestay_images");

                RoomTypeImage img = new RoomTypeImage();
                img.setRoomType(type);
                img.setImageUrl(url);
                img.setDisplayOrder(startOrder + i);

                imageEntities.add(img);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi upload ảnh");
            }
        }
        roomTypeImageRepository.saveAll(imageEntities);
    }

    @Transactional
    public void deleteRoomType(Long id) {
        RoomType type = roomTypeRepository.findByTypeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (roomRepository.existsByRoomType_TypeIdAndIsDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa loại phòng này vì vẫn còn phòng đang hoạt động bên trong.");
        }

        type.setIsDeleted(true);
        roomTypeRepository.save(type);
        sseService.sendNotification("ROOM_TYPE_UPDATE", id);
    }

    private void mapRoomTypeData(RoomType type, RoomTypeRequest req, HomestayRoomClass roomClass) {
        type.setName(req.getName());
        type.setDescription(req.getDescription());
        type.setBasePrice(req.getBasePrice());
        type.setMaxAdults(req.getMaxAdults());
        type.setMaxChildren(req.getMaxChildren());
        type.setRoomClass(roomClass);

        if (req.getAmenityIds() != null) {
            Set<HomestayAmenity> amenities = new HashSet<>(amenityRepository.findAllById(req.getAmenityIds()));
            type.setAmenities(amenities);
        }
    }

    @Transactional(readOnly = true)
    public Page<Room> getRooms(String keyword, String statusStr, Long typeId, Pageable pageable) {
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim().toLowerCase() + "%"
                : null;

        RoomStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = RoomStatus.valueOf(statusStr);
            } catch (Exception e) {
                status = null;
            }
        }

        return roomRepository.search(finalKeyword, status, typeId, pageable);
    }

    @Transactional
    public Room createRoom(RoomRequest req) {
        if (roomRepository.existsByRoomNumberIgnoreCaseAndIsDeletedFalse(req.getRoomNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số phòng đã tồn tại");
        }
        RoomType type = roomTypeRepository.findByTypeIdAndIsDeletedFalse(req.getTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loại phòng không tồn tại"));

        Room room = new Room();
        room.setRoomNumber(req.getRoomNumber());
        room.setRoomType(type);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setIsDeleted(false);
        Room saved = roomRepository.save(room);
        sseService.sendNotification("ROOM_UPDATE", saved.getRoomId());
        return saved;
    }

    @Transactional
    public Room updateRoom(Long id, RoomRequest req) {
        Room room = roomRepository.findByRoomIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String cleanRoomNumber = req.getRoomNumber().trim();

        if (roomRepository.existsByRoomNumberIgnoreCaseAndRoomIdNotAndIsDeletedFalse(cleanRoomNumber, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Số phòng '" + cleanRoomNumber + "' đã tồn tại.");
        }

        room.setRoomNumber(req.getRoomNumber());
        if (req.getTypeId() != null && !req.getTypeId().equals(room.getRoomType().getTypeId())) {
            RoomType type = roomTypeRepository.findByTypeIdAndIsDeletedFalse(req.getTypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            room.setRoomType(type);
        }

        Room saved = roomRepository.save(room);
        sseService.sendNotification("ROOM_UPDATE", id);
        return saved;
    }

    @Transactional
    public void updateRoomStatus(Long id, RoomStatus status) {
        Room room = roomRepository.findByRoomIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        room.setStatus(status);
        roomRepository.save(room);
        sseService.sendNotification("ROOM_UPDATE", id);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findByRoomIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (room.getStatus() == RoomStatus.BOOKED || room.getStatus() == RoomStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa phòng này vì đang có khách ở hoặc đã được đặt trước.");
        }

        room.setIsDeleted(true);
        roomRepository.save(room);
        sseService.sendNotification("ROOM_UPDATE", id);
    }

    private HomestayCommonStatus parseCommonStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty())
            return null;
        try {
            return HomestayCommonStatus.valueOf(statusStr);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminHomestayBookingResponse> getHomestayBookings(
            String keyword,
            BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        String searchKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        Page<HomestayBooking> pageResult = homestayBookingRepository.findBookingsForAdmin(
                status,
                fromDate,
                toDate,
                searchKeyword,
                pageable);

        return pageResult.map(this::mapToAdminListResponse);
    }

    private AdminHomestayBookingResponse mapToAdminListResponse(HomestayBooking booking) {
        return AdminHomestayBookingResponse.builder()
                .bookingId(booking.getBookingId())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "Chưa xếp phòng")
                .roomClassName(booking.getRoomClassNameSnapshot())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminHomestayBookingDetailResponse getHomestayBookingDetail(Long bookingId) {
        HomestayBooking booking = homestayBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt phòng không tồn tại"));

        return mapToAdminDetailResponse(booking);
    }

    private AdminHomestayBookingDetailResponse mapToAdminDetailResponse(HomestayBooking booking) {

        return AdminHomestayBookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())

                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())

                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "Chưa xếp phòng")
                .roomClassName(booking.getRoomClassNameSnapshot())
                .roomName(booking.getRoomNameSnapshot())
                .roomImage(booking.getRoomImageSnapshot())

                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfAdults(booking.getNumberOfAdults())
                .numberOfChildren(booking.getNumberOfChildren())

                .paymentMethod(booking.getPaymentMethod().name())
                .paymentTime(booking.getPaymentTime())
                .vnpTxnRef(booking.getVnpTxnRef())

                .pricePerNight(booking.getPricePerNightSnapshot() != null ? booking.getPricePerNightSnapshot()
                        : BigDecimal.ZERO)
                .subTotal(booking.getSubTotal() != null ? booking.getSubTotal() : BigDecimal.ZERO)
                .discountAmount(booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO)
                .depositAmount(booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO)
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO)
                .build();
    }

    @Transactional
    public AdminHomestayBookingDetailResponse updateHomestayBookingStatus(Long bookingId, BookingStatus newStatus,
            LocalDate confirmCheckOutDate, String newCouponCode) {

        HomestayBooking booking = homestayBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt phòng không tồn tại"));

        BookingStatus oldStatus = booking.getStatus();

        if (newStatus == BookingStatus.CHECKED_IN && oldStatus != BookingStatus.CHECKED_IN) {
            Room room = booking.getRoom();
            if (room != null && room.getStatus() != RoomStatus.AVAILABLE) {
                String reason = room.getStatus() == RoomStatus.OCCUPIED ? "đang có khách"
                        : "đang không khả dụng/bảo trì";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Phòng này hiện " + reason + ". Không thể Check-in!");
            }
        }

        if (newStatus == BookingStatus.COMPLETED && confirmCheckOutDate != null) {
            if (confirmCheckOutDate.isBefore(booking.getCheckInDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ngày check-out thực tế không thể trước ngày check-in.");
            }

            boolean isDateChanged = !confirmCheckOutDate.equals(booking.getCheckOutDate());
            boolean isCouponChanged = (newCouponCode != null);

            if (isDateChanged || isCouponChanged) {
                if (isDateChanged) {
                    booking.setCheckOutDate(confirmCheckOutDate);
                }
                recalculateBookingFinancials(booking, newCouponCode);
            }
        }

        booking.setStatus(newStatus);

        if (booking.getRoom() != null) {
            Room room = booking.getRoom();
            boolean isRoomUpdated = false;

            if (newStatus == BookingStatus.CHECKED_IN && oldStatus != BookingStatus.CHECKED_IN) {
                room.setStatus(RoomStatus.OCCUPIED);
                isRoomUpdated = true;
            } else if ((newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED)
                    && (oldStatus != BookingStatus.COMPLETED && oldStatus != BookingStatus.CANCELLED)) {
                room.setStatus(RoomStatus.AVAILABLE);
                isRoomUpdated = true;
            }

            if (isRoomUpdated) {
                roomRepository.save(room);
                sseService.sendNotification("HOMESTAY_ROOM_UPDATE", room.getRoomId());
            }
        }

        HomestayBooking saved = homestayBookingRepository.save(booking);
        sseService.sendNotification("HOMESTAY_BOOKING_UPDATE", saved.getBookingId());

        return mapToAdminDetailResponse(saved);
    }

    private void recalculateBookingFinancials(HomestayBooking booking, String newCouponCode) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (nights < 1)
            nights = 1;

        BigDecimal pricePerNight = booking.getPricePerNightSnapshot();
        BigDecimal newSubTotal = pricePerNight.multiply(BigDecimal.valueOf(nights));

        BigDecimal newDiscountAmount = BigDecimal.ZERO;
        String finalCouponCode = null;
        String oldCouponCode = booking.getAppliedCouponCode();

        boolean shouldRemoveCoupon = (newCouponCode != null && newCouponCode.trim().isEmpty());
        String codeToProcess = (newCouponCode != null && !newCouponCode.trim().isEmpty())
                ? newCouponCode.trim()
                : (shouldRemoveCoupon ? null : oldCouponCode);

        if (codeToProcess != null && !codeToProcess.isEmpty()) {
            Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsDeletedFalse(codeToProcess)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Mã giảm giá '" + codeToProcess + "' không tồn tại."));

            LocalDateTime now = LocalDateTime.now();

            if (coupon.getServiceType() != ServiceType.HOMESTAY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã không áp dụng cho Homestay.");
            }
            if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn.");
            }
            if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá chưa đến đợt áp dụng.");
            }

            if (coupon.getMinOrderValue() != null && newSubTotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tổng tiền mới (" + formatCurrency(newSubTotal) + ") thấp hơn điều kiện tối thiểu ("
                                + formatCurrency(coupon.getMinOrderValue()) + ") của mã " + codeToProcess);
            }

            boolean isSameAsOld = codeToProcess.equalsIgnoreCase(oldCouponCode);
            if (!isSameAsOld && coupon.getQuantity() != null && coupon.getUsedCount() >= coupon.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết lượt sử dụng.");
            }

            if (coupon.getDiscountPercent() != null) {
                newDiscountAmount = newSubTotal.multiply(BigDecimal.valueOf(coupon.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100));
                if (coupon.getMaxDiscountAmount() != null
                        && newDiscountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    newDiscountAmount = coupon.getMaxDiscountAmount();
                }
            } else {
                newDiscountAmount = coupon.getDiscountAmount();
            }
            if (newDiscountAmount.compareTo(newSubTotal) > 0)
                newDiscountAmount = newSubTotal;

            finalCouponCode = coupon.getCode();

            if (!isSameAsOld) {
                coupon.setUsedCount(coupon.getUsedCount() + 1);
                couponRepository.save(coupon);
                decrementOldCouponUsage(oldCouponCode);
            }

        } else if (shouldRemoveCoupon) {
            decrementOldCouponUsage(oldCouponCode);
            newDiscountAmount = BigDecimal.ZERO;
            finalCouponCode = null;
        }

        booking.setSubTotal(newSubTotal);
        booking.setDiscountAmount(newDiscountAmount);
        booking.setAppliedCouponCode(finalCouponCode);

        BigDecimal newTotalAmount = newSubTotal.subtract(newDiscountAmount);
        if (newTotalAmount.compareTo(BigDecimal.ZERO) < 0)
            newTotalAmount = BigDecimal.ZERO;

        booking.setTotalAmount(newTotalAmount);
    }

    @Transactional
    public void cancelHomestayBookingByAdmin(Long id) {
        String currentStaff = "System_Admin";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            currentStaff = auth.getName();
        }

        HomestayBooking booking = homestayBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn đặt phòng không tồn tại"));

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Đơn hàng đã hoàn tất, không thể hủy.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        boolean shouldRefund = false;

        boolean hasMonetaryTransaction = booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                && booking.getPaymentMethod() == PaymentMethod.VNPAY;

        if (hasMonetaryTransaction) {
            LocalDateTime checkInTime = booking.getCheckInDate().atTime(14, 0);
            int homestayDeadlineHours = settingsService.getHomestayCancellationDeadline();
            LocalDateTime deadline = checkInTime.minusHours(homestayDeadlineHours);

            boolean isWithinRefundPolicy = LocalDateTime.now().isBefore(deadline);

            if (isWithinRefundPolicy) {
                if (booking.getStatus() == BookingStatus.CONFIRMED) {
                    shouldRefund = true;
                } else if (booking.getStatus() == BookingStatus.PENDING) {
                    try {
                        String txnDate = booking.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        VNPayQueryResponse queryRes = paymentService.queryTransaction(booking.getVnpTxnRef(), txnDate);

                        if ("00".equals(queryRes.getResponseCode()) && "00".equals(queryRes.getTransactionStatus())) {
                            shouldRefund = true;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Lỗi kiểm tra trạng thái thanh toán VNPay. Vui lòng kiểm tra thủ công.");
                    }
                }
            }
        }

        if (shouldRefund) {
            try {
                LocalDateTime refundBaseTime = (booking.getPaymentTime() != null)
                        ? booking.getPaymentTime()
                        : booking.getCreatedAt();

                String transactionDate = refundBaseTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

                VNPayRefundResponse refundRes = paymentService.refundTransaction(
                        booking.getVnpTxnRef(),
                        booking.getDepositAmount(),
                        transactionDate,
                        currentStaff);

                if (!"00".equals(refundRes.getVnp_ResponseCode())) {
                    throw new RuntimeException("VNPay từ chối hoàn tiền: " + refundRes.getVnp_Message());
                }
            } catch (Exception e) {
                throw new RuntimeException("Lỗi xử lý hoàn tiền: " + e.getMessage());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        homestayBookingRepository.save(booking);

        sseService.sendNotification("HOMESTAY_BOOKING_UPDATE", id);

        try {
            emailService.sendBookingCancellationEmail(booking.getCustomerEmail(), booking);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email hủy phòng: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public HomestayOverviewResponse getHomestayPosOverview() {
        LocalDate today = LocalDate.now();

        List<HomestayBooking> arrivals = homestayBookingRepository.findAllByCheckInDateAndStatusNot(today,
                BookingStatus.CANCELLED);
        List<HomestayBooking> departures = homestayBookingRepository.findAllByCheckOutDateAndStatusNot(today,
                BookingStatus.CANCELLED);

        Map<Long, HomestayBooking> currentBookingMap = homestayBookingRepository
                .findAllByStatus(BookingStatus.CHECKED_IN).stream()
                .filter(b -> b.getRoom() != null)
                .collect(Collectors.toMap(
                        b -> b.getRoom().getRoomId(),
                        booking -> booking,
                        (existing, replacement) -> existing));

        List<HomestayRoomClass> allClasses = roomClassRepository.findAllByIsDeletedFalse();
        List<RoomType> allTypes = roomTypeRepository.findAllByIsDeletedFalse();
        List<Room> allRooms = roomRepository.findAllByIsDeletedFalse();

        Map<Long, List<Room>> roomsByTypeMap = allRooms.stream()
                .filter(r -> r.getRoomType() != null)
                .collect(Collectors.groupingBy(r -> r.getRoomType().getTypeId()));

        Map<Long, List<RoomType>> typesByClassMap = allTypes.stream()
                .filter(t -> t.getRoomClass() != null)
                .collect(Collectors.groupingBy(t -> t.getRoomClass().getClassId()));

        List<HomestayOverviewResponse.RoomClassGroup> roomMap = allClasses.stream()
                .sorted(Comparator.comparing(HomestayRoomClass::getName))
                .map(roomClass -> {
                    List<RoomType> typesInClass = typesByClassMap.getOrDefault(roomClass.getClassId(),
                            new ArrayList<>());

                    List<HomestayOverviewResponse.RoomTypeGroup> typeGroups = typesInClass.stream()
                            .sorted(Comparator.comparing(RoomType::getBasePrice))
                            .map(type -> {
                                List<Room> rooms = roomsByTypeMap.getOrDefault(type.getTypeId(), new ArrayList<>());

                                List<HomestayOverviewResponse.RoomSnapshot> roomSnapshots = rooms.stream()
                                        .sorted(Comparator.comparing(Room::getRoomNumber))
                                        .map(room -> {
                                            HomestayBooking currentBooking = currentBookingMap.get(room.getRoomId());

                                            return HomestayOverviewResponse.RoomSnapshot.builder()
                                                    .roomId(room.getRoomId())
                                                    .roomNumber(room.getRoomNumber())
                                                    .status(room.getStatus().name())
                                                    .currentBookingId(
                                                            currentBooking != null ? currentBooking.getBookingId()
                                                                    : null)
                                                    .currentCustomerName(
                                                            currentBooking != null ? currentBooking.getCustomerName()
                                                                    : null)
                                                    .checkInDate(
                                                            currentBooking != null ? currentBooking.getCheckInDate()
                                                                    : null)
                                                    .checkOutDate(
                                                            currentBooking != null ? currentBooking.getCheckOutDate()
                                                                    : null)
                                                    .build();
                                        })
                                        .collect(Collectors.toList());

                                return HomestayOverviewResponse.RoomTypeGroup.builder()
                                        .typeId(type.getTypeId())
                                        .typeName(type.getName())
                                        .pricePerNight(type.getBasePrice())
                                        .rooms(roomSnapshots)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return HomestayOverviewResponse.RoomClassGroup.builder()
                            .classId(roomClass.getClassId())
                            .className(roomClass.getName())
                            .roomTypes(typeGroups)
                            .build();
                })
                .collect(Collectors.toList());

        return HomestayOverviewResponse.builder()
                .arrivingToday(arrivals.stream().map(this::mapToShortBookingInfo).collect(Collectors.toList()))
                .departingToday(departures.stream().map(this::mapToShortBookingInfo).collect(Collectors.toList()))
                .roomMap(roomMap)
                .build();
    }

    private HomestayOverviewResponse.ShortBookingInfo mapToShortBookingInfo(HomestayBooking b) {
        return HomestayOverviewResponse.ShortBookingInfo.builder()
                .bookingId(b.getBookingId())
                .customerName(b.getCustomerName())
                .roomNumber(b.getRoom() != null ? b.getRoom().getRoomNumber() : "Chưa xếp")
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .build();
    }

    @Transactional
    public BookingCreationResponse createWalkInHomestayBooking(CreateHomestayBookingRequest request) {
        if (request.getRoomId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn phòng cụ thể cho khách vãng lai.");
        }

        List<Long> occupiedIds = homestayBookingRepository.findOccupiedRoomIds(request.getCheckInDate(),
                request.getCheckOutDate());

        if (occupiedIds.contains(request.getRoomId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng này đã có lịch đặt trong khoảng thời gian " + request.getCheckInDate() + " - "
                            + request.getCheckOutDate());
        }

        Room assignedRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại."));

        boolean isCheckInToday = request.getCheckInDate().equals(LocalDate.now());
        if (isCheckInToday && assignedRoom.getStatus() != RoomStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng " + assignedRoom.getRoomNumber()
                            + " hiện đang bẩn hoặc bảo trì, không thể nhận khách ngay.");
        }

        RoomType roomType = assignedRoom.getRoomType();

        HomestayBooking booking = new HomestayBooking();
        booking.setUser(null);

        booking.setRoom(assignedRoom);
        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setCustomerEmail(request.getCustomerEmail());

        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfAdults(request.getNumberOfAdults());
        booking.setNumberOfChildren(request.getNumberOfChildren());

        booking.setRoomNameSnapshot(roomType.getName());
        booking.setRoomClassNameSnapshot(roomType.getRoomClass() != null ? roomType.getRoomClass().getName() : "");
        booking.setPricePerNightSnapshot(roomType.getBasePrice());

        if (roomType.getImages() != null && !roomType.getImages().isEmpty()) {
            String mainImageUrl = roomType.getImages().stream()
                    .min(Comparator.comparing(RoomTypeImage::getDisplayOrder))
                    .map(RoomTypeImage::getImageUrl)
                    .orElse(null);
            booking.setRoomImageSnapshot(mainImageUrl);
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights < 1)
            nights = 1;

        BigDecimal subTotal = roomType.getBasePrice().multiply(BigDecimal.valueOf(nights));
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
        }

        BigDecimal finalTotal = subTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0)
            finalTotal = BigDecimal.ZERO;

        booking.setSubTotal(subTotal);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(finalTotal);

        booking.setPaymentMethod(PaymentMethod.CASH);

        if (isCheckInToday) {
            booking.setStatus(BookingStatus.CHECKED_IN);

            assignedRoom.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(assignedRoom);
            sseService.sendNotification("HOMESTAY_ROOM_UPDATE", assignedRoom.getRoomId());
        } else {
            booking.setStatus(BookingStatus.CONFIRMED);
        }

        booking.setDepositAmount(finalTotal);
        booking.setPaymentTime(LocalDateTime.now());

        HomestayBooking savedBooking = homestayBookingRepository.save(booking);
        sseService.sendNotification("HOMESTAY_BOOKING_UPDATE", savedBooking.getBookingId());

        return BookingCreationResponse.builder()
                .bookingCode(savedBooking.getAccessToken())
                .message("Tạo đơn vãng lai thành công. Đã thu tiền mặt.")
                .build();
    }

    private List<AdminAvailableRoomGroupResponse> findAvailableRoomsCore(
            LocalDate checkIn, LocalDate checkOut, Long excludeBookingId, Long currentRoomId) {

        List<Long> occupiedRoomIds;
        if (excludeBookingId != null) {
            occupiedRoomIds = homestayBookingRepository.findOccupiedRoomIdsExcludingBooking(checkIn, checkOut,
                    excludeBookingId);
        } else {
            occupiedRoomIds = homestayBookingRepository.findOccupiedRoomIds(checkIn, checkOut);
        }

        List<Room> allRooms = roomRepository.findAllByIsDeletedFalse();

        List<Room> candidateRooms = allRooms.stream()
                .filter(r -> r.getStatus() != RoomStatus.UNAVAILABLE)
                .collect(Collectors.toList());

        if (currentRoomId != null) {
            boolean currentExists = candidateRooms.stream().anyMatch(r -> r.getRoomId().equals(currentRoomId));
            if (!currentExists) {
                roomRepository.findById(currentRoomId).ifPresent(candidateRooms::add);
            }
        }

        List<Room> validRooms = candidateRooms.stream()
                .filter(room -> !occupiedRoomIds.contains(room.getRoomId()))
                .collect(Collectors.toList());

        Map<Long, List<Room>> roomsByClassId = validRooms.stream()
                .filter(r -> r.getRoomType().getRoomClass() != null)
                .collect(Collectors.groupingBy(r -> r.getRoomType().getRoomClass().getClassId()));

        return roomsByClassId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(classEntry -> {
                    Long classId = classEntry.getKey();
                    List<Room> roomsInClass = classEntry.getValue();
                    HomestayRoomClass roomClass = roomsInClass.get(0).getRoomType().getRoomClass();

                    Map<Long, List<Room>> roomsByTypeId = roomsInClass.stream()
                            .collect(Collectors.groupingBy(r -> r.getRoomType().getTypeId()));

                    List<AdminAvailableRoomGroupResponse.RoomTypeGroup> typeGroups = roomsByTypeId.entrySet().stream()
                            .map(typeEntry -> {
                                List<Room> roomsInType = typeEntry.getValue();
                                RoomType roomType = roomsInType.get(0).getRoomType();

                                List<AdminAvailableRoomGroupResponse.RoomDetail> roomDetails = roomsInType.stream()
                                        .sorted(Comparator.comparing(Room::getRoomNumber))
                                        .map(room -> AdminAvailableRoomGroupResponse.RoomDetail.builder()
                                                .roomId(room.getRoomId())
                                                .roomNumber(room.getRoomNumber())
                                                .isCurrentRoom(room.getRoomId().equals(currentRoomId))
                                                .build())
                                        .collect(Collectors.toList());

                                return AdminAvailableRoomGroupResponse.RoomTypeGroup.builder()
                                        .typeId(roomType.getTypeId())
                                        .typeName(roomType.getName())
                                        .pricePerNight(roomType.getBasePrice())
                                        .maxAdults(roomType.getMaxAdults())
                                        .maxChildren(roomType.getMaxChildren())
                                        .rooms(roomDetails)
                                        .build();
                            })
                            .sorted(Comparator
                                    .comparing(AdminAvailableRoomGroupResponse.RoomTypeGroup::getPricePerNight))
                            .collect(Collectors.toList());

                    return AdminAvailableRoomGroupResponse.builder()
                            .classId(classId)
                            .className(roomClass.getName())
                            .roomTypes(typeGroups)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminAvailableRoomGroupResponse> getAvailableRoomsForChange(Long bookingId) {
        HomestayBooking booking = homestayBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn không tồn tại"));

        Long currentRoomId = (booking.getRoom() != null) ? booking.getRoom().getRoomId() : null;

        return findAvailableRoomsCore(
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                bookingId,
                currentRoomId
        );
    }

    @Transactional(readOnly = true)
    public List<AdminAvailableRoomGroupResponse> getAvailableRoomsPublic(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ngày check-in và check-out");
        }
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày check-out phải sau ngày check-in");
        }

        return findAvailableRoomsCore(
                checkIn,
                checkOut,
                null,
                null
        );
    }

    @Transactional
    public AdminHomestayBookingDetailResponse changeRoom(Long bookingId, Long newRoomId, String newCouponCode) {
        HomestayBooking booking = homestayBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt phòng không tồn tại"));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể đổi phòng cho đơn đã hoàn tất hoặc bị hủy.");
        }

        Room oldRoom = booking.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng mới không tồn tại"));

        if (oldRoom != null && oldRoom.getRoomId().equals(newRoomId)) {
        } else {
            if (newRoom.getStatus() == RoomStatus.UNAVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Phòng " + newRoom.getRoomNumber()
                                + " hiện đang không khả dụng. Vui lòng chọn phòng khác.");
            }
        }

        List<Long> occupiedIds = homestayBookingRepository.findOccupiedRoomIdsExcludingBooking(
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                bookingId);

        if (occupiedIds.contains(newRoomId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng " + newRoom.getRoomNumber() + " đã có lịch đặt khác trong khoảng thời gian này.");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            if (newRoom.getStatus() != RoomStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Phòng " + newRoom.getRoomNumber() + " chưa sẵn sàng đón khách (Status: " + newRoom.getStatus()
                                + ").");
            }
            if (oldRoom != null && !oldRoom.getRoomId().equals(newRoomId)) {
                oldRoom.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(oldRoom);
                sseService.sendNotification("HOMESTAY_ROOM_UPDATE", oldRoom.getRoomId());
            }
            newRoom.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(newRoom);
            sseService.sendNotification("HOMESTAY_ROOM_UPDATE", newRoom.getRoomId());
        }

        booking.setRoom(newRoom);
        booking.setRoomNameSnapshot(newRoom.getRoomType().getName());
        booking.setRoomClassNameSnapshot(
                newRoom.getRoomType().getRoomClass() != null ? newRoom.getRoomType().getRoomClass().getName() : "");
        booking.setPricePerNightSnapshot(newRoom.getRoomType().getBasePrice());

        if (newRoom.getRoomType().getImages() != null && !newRoom.getRoomType().getImages().isEmpty()) {
            String mainImageUrl = newRoom.getRoomType().getImages().stream()
                    .min(Comparator.comparing(RoomTypeImage::getDisplayOrder))
                    .map(RoomTypeImage::getImageUrl)
                    .orElse(null);
            booking.setRoomImageSnapshot(mainImageUrl);
        }

        recalculateBookingFinancials(booking, newCouponCode);

        HomestayBooking savedBooking = homestayBookingRepository.save(booking);
        sseService.sendNotification("HOMESTAY_BOOKING_UPDATE", savedBooking.getBookingId());

        return mapToAdminDetailResponse(savedBooking);
    }

    private void decrementOldCouponUsage(String oldCode) {
        if (oldCode != null && !oldCode.isEmpty()) {
            couponRepository.findByCodeIgnoreCaseAndIsDeletedFalse(oldCode).ifPresent(c -> {
                if (c.getUsedCount() > 0) {
                    c.setUsedCount(c.getUsedCount() - 1);
                    couponRepository.save(c);
                }
            });
        }
    }

    private String formatCurrency(BigDecimal value) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(
                new Locale("vi", "VN"));
        df.applyPattern("#,###");
        return df.format(value) + " đ";
    }

    @Transactional(readOnly = true)
    public HomestayStatisticResponse getRevenueStatistics() {
        LocalDate today = LocalDate.now();

        LocalDate startOfToday = today;
        LocalDate endOfToday = today;

        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal revenueToday = homestayBookingRepository.sumRevenueByDateRange(startOfToday, endOfToday);
        long countToday = homestayBookingRepository.countCompletedByDateRange(startOfToday, endOfToday);

        BigDecimal revenueWeek = homestayBookingRepository.sumRevenueByDateRange(startOfWeek, endOfWeek);
        long countWeek = homestayBookingRepository.countCompletedByDateRange(startOfWeek, endOfWeek);

        BigDecimal revenueMonth = homestayBookingRepository.sumRevenueByDateRange(startOfMonth, endOfMonth);
        long countMonth = homestayBookingRepository.countCompletedByDateRange(startOfMonth, endOfMonth);

        BigDecimal revenueTotal = homestayBookingRepository.sumTotalRevenue();
        long countTotal = homestayBookingRepository.countTotalCompleted();

        return HomestayStatisticResponse.builder()
                .today(HomestayStatisticResponse.MetricDetail.builder()
                        .revenue(revenueToday)
                        .completedOrders(countToday)
                        .build())
                .thisWeek(HomestayStatisticResponse.MetricDetail.builder()
                        .revenue(revenueWeek)
                        .completedOrders(countWeek)
                        .build())
                .thisMonth(HomestayStatisticResponse.MetricDetail.builder()
                        .revenue(revenueMonth)
                        .completedOrders(countMonth)
                        .build())
                .total(HomestayStatisticResponse.MetricDetail.builder()
                        .revenue(revenueTotal)
                        .completedOrders(countTotal)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChartDataResponse> getRevenueChart(String type) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        List<Object[]> rawData;

        switch (type) {
            case "this_month":
                startDate = LocalDate.now().withDayOfMonth(1);
                rawData = homestayBookingRepository.getDailyRevenueChartData(startDate, endDate);
                break;

            case "monthly":
                startDate = LocalDate.now().withDayOfYear(1);
                rawData = homestayBookingRepository.getMonthlyRevenueChartData(startDate, endDate);
                break;

            case "this_week":
            default:
                startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                rawData = homestayBookingRepository.getDailyRevenueChartData(startDate, endDate);
                break;
        }

        return rawData.stream()
                .map(row -> new ChartDataResponse(
                        (String) row[0],
                        (row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}
