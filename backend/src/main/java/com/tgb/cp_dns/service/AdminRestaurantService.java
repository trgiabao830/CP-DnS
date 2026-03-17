package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.dto.payment.VNPayRefundResponse;
import com.tgb.cp_dns.dto.restaurant.*;
import com.tgb.cp_dns.entity.restaurant.*;
import com.tgb.cp_dns.enums.AreaStatus;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.BookingType;
import com.tgb.cp_dns.enums.CategoryStatus;
import com.tgb.cp_dns.enums.FoodStatus;
import com.tgb.cp_dns.enums.OptionStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.enums.TableStatus;
import com.tgb.cp_dns.repository.restaurant.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

@Service
@RequiredArgsConstructor
public class AdminRestaurantService {

    private final FoodCategoryRepository categoryRepository;
    private final FoodRepository foodRepository;
    private final FoodVariantOptionRepository optionRepository;
    private final RestaurantAreaRepository areaRepository;
    private final RestaurantTableRepository tableRepository;
    private final RestaurantBookingRepository bookingRepository;
    private final RestaurantOrderDetailRepository orderDetailRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final CloudinaryService cloudinaryService;
    private final SseNotificationService sseService;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public Page<FoodCategory> getAllCategories(String keyword, String statusStr, Pageable pageable) {
        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        CategoryStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = CategoryStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        return categoryRepository.searchCategories(finalKeyword, finalStatus, pageable);
    }

    @Transactional
    public FoodCategory createCategory(String name) {
        if (categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại.");
        }

        FoodCategory category = new FoodCategory();
        category.setName(name);
        Integer nextOrder = categoryRepository.findMaxDisplayOrder() + 1;
        category.setDisplayOrder(nextOrder);
        category.setStatus(CategoryStatus.AVAILABLE);

        FoodCategory saved = categoryRepository.save(category);
        sseService.sendNotification("FOOD_CATEGORY_UPDATE", saved.getCategoryId());
        return saved;
    }

    @Transactional
    public void updateCategory(Long id, CategoryRequest request) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        if (categoryRepository.existsByNameIgnoreCaseAndCategoryIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại.");
        }

        category.setName(request.getName());
        categoryRepository.save(category);
        sseService.sendNotification("FOOD_CATEGORY_UPDATE", id);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (foodRepository.existsByCategory_CategoryIdAndIsDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa danh mục này vì vẫn còn món ăn đang hoạt động bên trong");
        }

        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));
        category.setIsDeleted(true);
        categoryRepository.save(category);
        sseService.sendNotification("FOOD_CATEGORY_UPDATE", id);
    }

    @Transactional
    public void updateCategoryStatus(Long id, CategoryStatus status) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        category.setStatus(status);
        categoryRepository.save(category);
        sseService.sendNotification("FOOD_CATEGORY_UPDATE", id);
    }

    @Transactional
    public void reorderCategories(List<ReorderItem> items) {
        for (ReorderItem item : items) {
            FoodCategory category = categoryRepository.findById(item.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Danh mục ID " + item.getId() + " không tồn tại"));
            category.setDisplayOrder(item.getOrder());
            categoryRepository.save(category);
            sseService.sendNotification("FOOD_CATEGORY_UPDATE", "REORDER");
        }
    }

    @Transactional(readOnly = true)
    public Page<RestaurantArea> getAllAreas(String keyword, String statusStr, Pageable pageable) {
        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        AreaStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = AreaStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        return areaRepository.searchAreas(finalKeyword, finalStatus, pageable);
    }

    @Transactional
    public void createArea(RestaurantAreaRequest request) {
        if (areaRepository.existsByNameIgnoreCaseAndIsDeletedFalse(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên khu vực đã tồn tại.");
        }

        RestaurantArea area = new RestaurantArea();
        area.setName(request.getName());
        area.setStatus(AreaStatus.AVAILABLE);
        RestaurantArea saved = areaRepository.save(area);
        sseService.sendNotification("RESTAURANT_AREA_UPDATE", saved.getAreaId());
    }

    @Transactional
    public void updateArea(Long id, RestaurantAreaRequest request) {
        RestaurantArea area = areaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        if (areaRepository.existsByNameIgnoreCaseAndAreaIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên khu vực đã tồn tại.");
        }

        area.setName(request.getName());
        areaRepository.save(area);
        sseService.sendNotification("RESTAURANT_AREA_UPDATE", id);
    }

    @Transactional
    public void deleteArea(Long id) {
        if (tableRepository.existsByArea_AreaIdAndIsDeletedFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa khu vực này vì vẫn còn bàn đang hoạt động bên trong.");
        }

        RestaurantArea area = areaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));
        area.setIsDeleted(true);
        areaRepository.save(area);
        sseService.sendNotification("RESTAURANT_AREA_UPDATE", id);
    }

    @Transactional
    public void updateAreaStatus(Long id, AreaStatus status) {
        RestaurantArea area = areaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        area.setStatus(status);
        areaRepository.save(area);
        sseService.sendNotification("RESTAURANT_AREA_UPDATE", id);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantTable> getAllTables(String keyword, String statusStr, Long areaId, Pageable pageable) {
        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        TableStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = TableStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        return tableRepository.searchTables(finalKeyword, finalStatus, areaId, pageable);
    }

    @Transactional
    public void createTable(RestaurantTableRequest request) {
        if (tableRepository.existsByTableNumberIgnoreCaseAndIsDeletedFalse(request.getTableNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số bàn đã tồn tại.");
        }

        RestaurantArea area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setArea(area);
        table.setStatus(TableStatus.AVAILABLE);
        RestaurantTable saved = tableRepository.save(table);
        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", saved.getTableId());
    }

    @Transactional
    public void updateTable(Long id, RestaurantTableRequest request) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        if (tableRepository.existsByTableNumberIgnoreCaseAndTableIdNotAndIsDeletedFalse(request.getTableNumber(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số bàn đã tồn tại.");
        }

        if (!table.getArea().getAreaId().equals(request.getAreaId())) {
            RestaurantArea newArea = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực mới không tồn tại"));
            table.setArea(newArea);
        }

        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        tableRepository.save(table);
        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", id);
    }

    @Transactional
    public void deleteTable(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        if (table.getStatus() == TableStatus.RESERVED || table.getStatus() == TableStatus.SERVING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa bàn này vì đang có khách hoặc đã được đặt trước.");
        }

        table.setIsDeleted(true);
        table.setStatus(TableStatus.UNAVAILABLE);
        tableRepository.save(table);
        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", id);
    }

    @Transactional
    public void updateTableStatus(Long id, TableStatus status) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        table.setStatus(status);
        tableRepository.save(table);
        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", id);
    }

    @Transactional(readOnly = true)
    public Page<FoodResponse> getAllFoods(String keyword, String statusStr, Long categoryId, Pageable pageable) {
        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        FoodStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = FoodStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        Page<Food> foodPage = foodRepository.searchFoods(
                categoryId,
                finalKeyword,
                finalStatus,
                pageable);

        return foodPage.map(this::mapToFoodResponse);
    }

    @Transactional(readOnly = true)
    public Page<FoodResponse> getFoodsByCategory(Long categoryId, String keyword, String statusStr, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại");
        }

        String finalKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            finalKeyword = "%" + keyword.trim().toLowerCase() + "%";
        }

        FoodStatus finalStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                finalStatus = FoodStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                finalStatus = null;
            }
        }

        Page<Food> foodPage = foodRepository.searchFoodsByCategory(
                categoryId,
                finalKeyword,
                finalStatus,
                pageable);

        return foodPage.map(this::mapToFoodResponse);
    }

    @Transactional(readOnly = true)
    public FoodResponse getFoodDetail(Long id) {
        Food food = foodRepository.findById(id)
                .filter(f -> !f.getIsDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        return mapToFoodResponse(food);
    }

    @Transactional
    public void createFood(FoodRequest request, MultipartFile imageFile) {
        if (foodRepository.existsByNameIgnoreCaseAndIsDeletedFalse(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại.");
        }

        validateComboComposition(request.getVariants(), null);

        FoodCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        String finalImageUrl = request.getImageUrl();

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = cloudinaryService.uploadImage(imageFile, request.getName(), "food_images");
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Lỗi upload ảnh: " + e.getMessage());
            }
        }

        Integer nextOrder = foodRepository.findMaxDisplayOrder(category.getCategoryId()) + 1;

        Food food = new Food();
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setBasePrice(request.getBasePrice());
        food.setDiscountPrice(request.getDiscountPrice());

        food.setImageUrl(finalImageUrl);

        food.setDisplayOrder(nextOrder);
        food.setStatus(FoodStatus.AVAILABLE);
        food.setCategory(category);

        mergeVariants(food, request.getVariants());

        Food saved = foodRepository.save(food);
        sseService.sendNotification("FOOD_UPDATE", saved.getFoodId());
    }

    @Transactional
    public void updateFood(Long id, FoodRequest request, MultipartFile imageFile) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        if (foodRepository.existsByNameIgnoreCaseAndFoodIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại.");
        }

        validateComboComposition(request.getVariants(), null);

        if (request.getCategoryId() != null
                && !food.getCategory().getCategoryId().equals(request.getCategoryId())) {

            FoodCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục mới không tồn tại"));

            Integer nextOrder = foodRepository.findMaxDisplayOrder(newCategory.getCategoryId()) + 1;
            food.setCategory(newCategory);
            food.setDisplayOrder(nextOrder);
        }

        String currentImageUrl = request.getImageUrl();

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                currentImageUrl = cloudinaryService.uploadImage(imageFile, request.getName(), "food_images");
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi upload ảnh cập nhật");
            }
        }
        food.setImageUrl(currentImageUrl);

        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setBasePrice(request.getBasePrice());
        food.setDiscountPrice(request.getDiscountPrice());

        mergeVariants(food, request.getVariants());
        foodRepository.save(food);
        sseService.sendNotification("FOOD_UPDATE", id);
    }

    @Transactional
    public void updateFoodStatus(Long id, FoodStatus status) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        food.setStatus(status);
        foodRepository.save(food);
        sseService.sendNotification("FOOD_UPDATE", id);
    }

    @Transactional
    public void deleteFood(Long id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        List<String> parentCombos = foodRepository.findComboNamesByLinkedFoodId(id);

        if (!parentCombos.isEmpty()) {
            String comboNames = String.join(", ", parentCombos);

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể xóa món '" + food.getName() + "' vì đang là thành phần của các Combo: ["
                            + comboNames + "]. Vui lòng xóa món này khỏi các Combo trên trước.");
        }

        food.setIsDeleted(true);
        food.setStatus(FoodStatus.UNAVAILABLE);

        foodRepository.save(food);
        sseService.sendNotification("FOOD_UPDATE", id);
    }

    @Transactional
    public void reorderFoods(List<ReorderItem> items) {
        for (ReorderItem item : items) {
            Food food = foodRepository.findById(item.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Món ăn ID " + item.getId() + " không tồn tại"));
            food.setDisplayOrder(item.getOrder());
            foodRepository.save(food);
        }
        sseService.sendNotification("FOOD_UPDATE", "REORDER");
    }

    private FoodResponse mapToFoodResponse(Food food) {
        return FoodResponse.builder()
                .foodId(food.getFoodId())
                .name(food.getName())
                .description(food.getDescription())
                .basePrice(food.getBasePrice())
                .discountPrice(food.getDiscountPrice())
                .imageUrl(food.getImageUrl())
                .status(food.getStatus().name())
                .categoryId(food.getCategory() != null ? food.getCategory().getCategoryId() : null)
                .variants(food.getVariants().stream()
                        .filter(v -> !v.getIsDeleted())
                        .sorted(Comparator.comparing(FoodVariant::getVariantId))
                        .map(this::mapToVariantDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private FoodResponse.VariantDto mapToVariantDto(FoodVariant variant) {
        return FoodResponse.VariantDto.builder()
                .variantId(variant.getVariantId())
                .name(variant.getName())
                .isRequired(variant.getIsRequired())
                .options(variant.getOptions().stream()
                        .filter(o -> !o.getIsDeleted())

                        .filter(o -> {
                            if (o.getLinkedFood() == null) {
                                return true;
                            }
                            return !o.getLinkedFood().getIsDeleted();
                        })
                        .sorted(Comparator.comparing(FoodVariantOption::getOptionId))
                        .map(this::mapToOptionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private FoodResponse.OptionDto mapToOptionDto(FoodVariantOption option) {
        String finalStatus = option.getStatus().name();
        List<FoodResponse.VariantDto> childrenVariants = new ArrayList<>();

        Long linkedCategoryId = null;

        if (option.getLinkedFood() != null) {
            Food linked = option.getLinkedFood();

            if (linked.getCategory() != null) {
                linkedCategoryId = linked.getCategory().getCategoryId();
            }

            if (linked.getIsDeleted() || linked.getStatus() != FoodStatus.AVAILABLE) {
                finalStatus = "UNAVAILABLE";
            } else {
                childrenVariants = linked.getVariants().stream()
                        .filter(v -> !v.getIsDeleted())
                        .map(this::mapToVariantDto)
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
                .linkedVariants(childrenVariants)
                .build();
    }

    private void mergeOptions(FoodVariant variant, List<VariantOptionRequest> optionRequests) {
        if (optionRequests == null)
            optionRequests = new ArrayList<>();

        Set<Long> reqOptionIds = optionRequests.stream()
                .filter(req -> req.getOptionId() != null)
                .map(VariantOptionRequest::getOptionId)
                .collect(Collectors.toSet());

        for (FoodVariantOption oldOption : variant.getOptions()) {
            if (!reqOptionIds.contains(oldOption.getOptionId())) {
                oldOption.setIsDeleted(true);
            }
        }

        Set<String> namesInRequest = new HashSet<>();

        for (VariantOptionRequest req : optionRequests) {
            String cleanName = req.getName().trim();

            if (namesInRequest.contains(cleanName.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tùy chọn '" + cleanName + "' bị trùng lặp trong danh sách gửi lên.");
            }
            namesInRequest.add(cleanName.toLowerCase());

            boolean isDuplicateNameDb = variant.getOptions().stream()
                    .anyMatch(o -> !o.getIsDeleted()
                            && o.getName().equalsIgnoreCase(cleanName)
                            && (req.getOptionId() == null || !o.getOptionId().equals(req.getOptionId())));

            if (isDuplicateNameDb) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tùy chọn '" + cleanName + "' đã tồn tại trong nhóm này.");
            }

            OptionStatus statusToSet = (req.getStatus() != null)
                    ? req.getStatus()
                    : OptionStatus.AVAILABLE;

            Food linkedFood = null;
            if (req.getLinkedFoodId() != null) {
                linkedFood = foodRepository.findById(req.getLinkedFoodId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Món ăn liên kết ID " + req.getLinkedFoodId() + " không tồn tại"));
            }

            if (req.getOptionId() == null) {
                FoodVariantOption newOption = new FoodVariantOption();
                newOption.setName(req.getName());
                newOption.setPriceAdjustment(req.getPriceAdjustment());
                newOption.setVariant(variant);
                newOption.setStatus(statusToSet);
                newOption.setLinkedFood(linkedFood);
                newOption.setIsDeleted(false);
                variant.getOptions().add(newOption);
            } else {
                Food finalLinkedFood = linkedFood;

                variant.getOptions().stream()
                        .filter(o -> o.getOptionId().equals(req.getOptionId()))
                        .findFirst()
                        .ifPresent(existingOption -> {
                            existingOption.setName(req.getName());
                            existingOption.setPriceAdjustment(req.getPriceAdjustment());
                            existingOption.setStatus(statusToSet);
                            existingOption.setLinkedFood(finalLinkedFood);
                            existingOption.setIsDeleted(false);
                        });
            }
        }
    }

    private void mergeVariants(Food food, List<VariantRequest> variantRequests) {
        if (variantRequests == null)
            variantRequests = new ArrayList<>();

        Set<Long> reqVariantIds = variantRequests.stream()
                .filter(req -> req.getVariantId() != null)
                .map(VariantRequest::getVariantId)
                .collect(Collectors.toSet());

        for (FoodVariant oldVariant : food.getVariants()) {
            if (!reqVariantIds.contains(oldVariant.getVariantId())) {
                oldVariant.setIsDeleted(true);
                oldVariant.getOptions().forEach(o -> o.setIsDeleted(true));
            }
        }

        Set<String> namesInRequest = new HashSet<>();

        for (VariantRequest req : variantRequests) {
            String cleanName = req.getName().trim();

            if (namesInRequest.contains(cleanName.toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nhóm tùy chọn '" + cleanName + "' bị trùng lặp trong danh sách gửi lên.");
            }
            namesInRequest.add(cleanName.toLowerCase());

            boolean isDuplicateNameDb = food.getVariants().stream()
                    .anyMatch(v -> !v.getIsDeleted()
                            && v.getName().equalsIgnoreCase(cleanName)
                            && (req.getVariantId() == null || !v.getVariantId().equals(req.getVariantId())));

            if (isDuplicateNameDb) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nhóm tùy chọn '" + cleanName + "' đã tồn tại trong món ăn này.");
            }
            if (req.getVariantId() == null) {
                FoodVariant newVariant = new FoodVariant();
                newVariant.setName(cleanName);
                newVariant.setIsRequired(req.getIsRequired());
                newVariant.setFood(food);
                newVariant.setIsDeleted(false);

                mergeOptions(newVariant, req.getOptions());

                food.getVariants().add(newVariant);
            } else {
                food.getVariants().stream()
                        .filter(v -> v.getVariantId().equals(req.getVariantId()))
                        .findFirst()
                        .ifPresent(existingVariant -> {
                            existingVariant.setName(cleanName);
                            existingVariant.setIsRequired(req.getIsRequired());
                            existingVariant.setIsDeleted(false);

                            mergeOptions(existingVariant, req.getOptions());
                        });
            }
        }
    }

    private void validateComboComposition(List<VariantRequest> variants, Long currentFoodId) {
        if (variants == null || variants.isEmpty())
            return;

        Set<Long> linkedFoodIds = new HashSet<>();
        for (VariantRequest v : variants) {
            if (v.getOptions() != null) {
                for (VariantOptionRequest o : v.getOptions()) {
                    if (o.getLinkedFoodId() != null) {
                        if (currentFoodId != null && o.getLinkedFoodId().equals(currentFoodId)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Món ăn không thể chứa chính nó.");
                        }
                        linkedFoodIds.add(o.getLinkedFoodId());
                    }
                }
            }
        }

        if (linkedFoodIds.isEmpty())
            return;

        List<Food> linkedFoods = foodRepository.findAllWithDetailsByIds(linkedFoodIds);
        Map<Long, Food> foodMap = linkedFoods.stream()
                .collect(Collectors.toMap(Food::getFoodId, f -> f));

        for (Long linkedId : linkedFoodIds) {
            if (!foodMap.containsKey(linkedId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Món ăn liên kết (ID: " + linkedId + ") không tồn tại.");
            }

            Food linkedFood = foodMap.get(linkedId);

            boolean isLinkedFoodACombo = linkedFood.getVariants().stream()
                    .filter(v -> !v.getIsDeleted())
                    .flatMap(v -> v.getOptions().stream())
                    .filter(o -> !o.getIsDeleted())
                    .anyMatch(o -> o.getLinkedFood() != null);

            if (isLinkedFoodACombo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không cho phép Combo lồng nhau. Món '" + linkedFood.getName() + "' là một Combo.");
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminBookingResponse> getBookings(
            String keyword,
            BookingStatus status,
            LocalDate fromDateStr,
            LocalDate toDateStr,
            Pageable pageable) {

        String searchPattern = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchPattern = "%" + keyword.trim().toLowerCase() + "%";
        }

        LocalDateTime fromDateTime = (fromDateStr != null) ? fromDateStr.atStartOfDay() : null;
        LocalDateTime toDateTime = (toDateStr != null) ? toDateStr.atTime(LocalTime.MAX) : null;

        Page<RestaurantBooking> entities = bookingRepository.findBookingsForAdmin(
                status, fromDateTime, toDateTime, searchPattern, pageable);

        return entities.map(this::mapToAdminResponse);
    }

    private AdminBookingResponse mapToAdminResponse(RestaurantBooking entity) {
        return AdminBookingResponse.builder()
                .bookingId(entity.getBookingId())
                .customerName(entity.getCustomerName())
                .customerPhone(entity.getCustomerPhone())
                .customerEmail(entity.getCustomerEmail())
                .bookingTime(entity.getBookingTime())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus().name())
                .tableNumber(entity.getTable() != null ? entity.getTable().getTableNumber() : "Chưa xếp")
                .numberOfGuests(entity.getNumberOfGuests())
                .totalAmount(entity.getTotalAmount())
                .depositAmount(entity.getDepositAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailResponse getBookingDetail(Long bookingId) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt bàn không tồn tại"));

        return mapToAdminDetailResponse(booking);
    }

    private AdminBookingDetailResponse mapToAdminDetailResponse(RestaurantBooking booking) {

        List<AdminBookingDetailResponse.AdminOrderedItemDto> items = new ArrayList<>();
        if (booking.getOrderDetails() != null) {
            items = booking.getOrderDetails().stream().map(d -> {
                List<String> optNames = d.getSelectedOptions().stream()
                        .map(o -> o.getOptionNameSnapshot() + ": " + o.getVariantNameSnapshot())
                        .collect(Collectors.toList());

                return AdminBookingDetailResponse.AdminOrderedItemDto.builder()
                        .detailId(d.getDetailId())
                        .foodId(d.getFood().getFoodId())
                        .foodName(d.getFoodNameSnapshot())
                        .foodImage(d.getFoodImageSnapshot())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .totalPrice(d.getTotalPrice())
                        .note(d.getNote())
                        .options(optNames)
                        .build();
            }).collect(Collectors.toList());
        }

        return AdminBookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus().name())
                .bookingType(booking.getBookingType().name())
                .createdAt(booking.getCreatedAt())

                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())

                .tableNumber(booking.getTable() != null ? booking.getTable().getTableNumber() : "Chưa xếp bàn")
                .numberOfGuests(booking.getNumberOfGuests())
                .bookingTime(booking.getBookingTime())

                .paymentMethod(booking.getPaymentMethod().name())
                .paymentTime(booking.getPaymentTime())
                .vnpTxnRef(booking.getVnpTxnRef())

                .subTotal(booking.getSubTotal())
                .discountAmount(booking.getDiscountAmount())
                .depositAmount(booking.getDepositAmount())
                .totalAmount(booking.getTotalAmount())

                .orderItems(items)
                .build();
    }

    @Transactional
    public AdminBookingDetailResponse updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt bàn không tồn tại"));

        booking.setStatus(newStatus);

        if (newStatus == BookingStatus.SERVING && booking.getTable() != null) {
            booking.getTable().setStatus(TableStatus.SERVING);
            tableRepository.save(booking.getTable());
            sseService.sendNotification("RESTAURANT_TABLE_UPDATE", booking.getTable().getTableId());
        }

        if ((newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED)
                && booking.getTable() != null) {
            booking.getTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(booking.getTable());
            sseService.sendNotification("RESTAURANT_TABLE_UPDATE", booking.getTable().getTableId());
        }

        RestaurantBooking saved = bookingRepository.save(booking);

        sseService.sendNotification("BOOKING_UPDATE", saved.getBookingId());

        return mapToAdminDetailResponse(saved);
    }

    @Transactional
    public void cancelBookingByAdmin(Long id) {
        String currentStaff = "System_Admin";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            currentStaff = auth.getName();
        }

        RestaurantBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đơn đặt bàn không tồn tại"));

        if (booking.getStatus() == BookingStatus.SERVING || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Đơn hàng đang phục vụ hoặc đã hoàn tất.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        boolean shouldRefund = false;

        boolean hasMonetaryTransaction = booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                && booking.getPaymentMethod() == PaymentMethod.VNPAY;

        if (hasMonetaryTransaction) {
            int depositDeadlineHours = settingsService.getRestaurantCancellationDeadlineWithDeposit();
            LocalDateTime deadline = booking.getBookingTime().minusHours(depositDeadlineHours);

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
                String transactionDate = booking.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
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
        bookingRepository.save(booking);

        sseService.sendNotification("BOOKING_UPDATE", id);

        try {
            emailService.sendBookingCancellationEmail(booking.getCustomerEmail(), booking);
        } catch (Exception e) {
        }
    }

    @Transactional(readOnly = true)
    public RestaurantOverviewResponse getDailyOverview() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<RestaurantArea> allAreas = areaRepository.findAllByIsDeletedFalse();
        List<RestaurantTable> allTables = tableRepository.findAllByIsDeletedFalse();
        List<RestaurantBooking> todayBookings = bookingRepository.findAllByDateRange(startOfDay, endOfDay);

        Map<Long, List<ShortBookingDto>> bookingsByTableMap = todayBookings.stream()
                .filter(b -> b.getTable() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getTable().getTableId(),
                        Collectors.mapping(this::mapToShortDto, Collectors.toList())));

        Map<Long, List<RestaurantTable>> tablesByAreaMap = allTables.stream()
                .collect(Collectors.groupingBy(t -> t.getArea().getAreaId()));

        List<AreaSnapshotDto> areadtos = allAreas.stream()
                .sorted(Comparator.comparing(RestaurantArea::getAreaId))
                .map(area -> mapToAreaSnapshot(area, tablesByAreaMap, bookingsByTableMap))
                .collect(Collectors.toList());

        return RestaurantOverviewResponse.builder()
                .areas(areadtos)
                .build();
    }

    private AreaSnapshotDto mapToAreaSnapshot(
            RestaurantArea area,
            Map<Long, List<RestaurantTable>> tablesByAreaMap,
            Map<Long, List<ShortBookingDto>> bookingsByTableMap) {
        List<RestaurantTable> tablesInArea = tablesByAreaMap.getOrDefault(area.getAreaId(), new ArrayList<>());

        tablesInArea.sort(Comparator.comparing(RestaurantTable::getTableNumber));

        List<TableSnapshotDto> tableDtos = tablesInArea.stream()
                .map(table -> mapToTableSnapshot(table, bookingsByTableMap))
                .collect(Collectors.toList());

        return AreaSnapshotDto.builder()
                .areaId(area.getAreaId())
                .areaName(area.getName())
                .tables(tableDtos)
                .build();
    }

    private TableSnapshotDto mapToTableSnapshot(
            RestaurantTable table,
            Map<Long, List<ShortBookingDto>> bookingsByTableMap) {
        List<ShortBookingDto> bookings = bookingsByTableMap.getOrDefault(table.getTableId(), new ArrayList<>());

        ShortBookingDto currentBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.SERVING)
                .findFirst()
                .orElse(null);

        return TableSnapshotDto.builder()
                .tableId(table.getTableId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .currentStatus(table.getStatus())
                .todayBookings(bookings)
                .currentBooking(currentBooking)
                .build();
    }

    private ShortBookingDto mapToShortDto(RestaurantBooking entity) {
        return ShortBookingDto.builder()
                .bookingId(entity.getBookingId())
                .customerName(entity.getCustomerName())
                .customerPhone(entity.getCustomerPhone())
                .bookingTime(entity.getBookingTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .numberOfGuests(entity.getNumberOfGuests())
                .build();
    }

    @Transactional
    public AdminBookingDetailResponse createWalkInBooking(WalkInBookingRequest request) {
        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn không tồn tại"));

        if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn này hiện không trống.");
        }

        RestaurantBooking booking = new RestaurantBooking();
        booking.setTable(table);

        booking.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : "Khách vãng lai");
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setNumberOfGuests(request.getNumberOfGuests() != null ? request.getNumberOfGuests() : 1);

        LocalDateTime now = LocalDateTime.now();
        booking.setBookingTime(now);
        booking.setEndTime(now.plusHours(2));

        booking.setStatus(BookingStatus.SERVING);
        booking.setBookingType(BookingType.WALK_IN);
        booking.setPaymentMethod(PaymentMethod.CASH);

        table.setStatus(TableStatus.SERVING);
        tableRepository.save(table);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<RestaurantOrderDetail> details = new ArrayList<>();

        if (request.getOrderItems() != null) {
            for (OrderItemRequest itemReq : request.getOrderItems()) {
                RestaurantOrderDetail detail = createOrderDetail(booking, itemReq, true);

                details.add(detail);
                totalAmount = totalAmount.add(detail.getTotalPrice());
            }
        }

        booking.setOrderDetails(details);

        booking.setOrderDetails(details);
        booking.setTotalAmount(totalAmount);
        booking.setDepositAmount(BigDecimal.ZERO);

        RestaurantBooking savedBooking = bookingRepository.save(booking);

        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", table.getTableId());

        return mapToAdminDetailResponse(savedBooking);
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

    @Transactional
    public AdminBookingDetailResponse addOrderItem(Long bookingId, OrderItemRequest itemReq) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn không tồn tại"));

        Food food = foodRepository.findById(itemReq.getFoodId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Món ăn ID " + itemReq.getFoodId() + " không tồn tại"));

        if (Boolean.TRUE.equals(food.getIsDeleted()) || food.getStatus() == FoodStatus.UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Món '" + food.getName() + "' đã ngừng kinh doanh.");
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

        detail = orderDetailRepository.save(detail);

        if (booking.getOrderDetails() == null) {
            booking.setOrderDetails(new ArrayList<>());
        }
        booking.getOrderDetails().add(detail);

        recalculateBookingTotal(booking);

        RestaurantBooking savedBooking = bookingRepository.save(booking);

        sseService.sendNotification("BOOKING_UPDATE", savedBooking.getBookingId());

        return mapToAdminDetailResponse(savedBooking);
    }

    @Transactional
    public AdminBookingDetailResponse updateOrderItemQuantity(Long bookingId, Long detailId, int newQuantity) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn không tồn tại"));

        RestaurantOrderDetail detail = booking.getOrderDetails().stream()
                .filter(d -> d.getDetailId().equals(detailId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chi tiết đơn không tồn tại"));

        if (newQuantity <= 0) {
            booking.getOrderDetails().remove(detail);
        } else {
            detail.setQuantity(newQuantity);

            BigDecimal totalOptionPrice = BigDecimal.ZERO;
            if (detail.getSelectedOptions() != null) {
                for (RestaurantOrderOption opt : detail.getSelectedOptions()) {
                    if (opt.getPriceAdjustmentSnapshot() != null) {
                        totalOptionPrice = totalOptionPrice.add(opt.getPriceAdjustmentSnapshot());
                    }
                }
            }

            BigDecimal newLineTotal = detail.getUnitPrice().add(totalOptionPrice)
                    .multiply(BigDecimal.valueOf(newQuantity));

            detail.setTotalPrice(newLineTotal);
        }

        recalculateBookingTotal(booking);

        RestaurantBooking savedBooking = bookingRepository.save(booking);
        sseService.sendNotification("BOOKING_UPDATE", savedBooking.getBookingId());

        return mapToAdminDetailResponse(savedBooking);
    }

    private void recalculateBookingTotal(RestaurantBooking booking) {
        BigDecimal subTotal = BigDecimal.ZERO;

        if (booking.getOrderDetails() != null) {
            for (RestaurantOrderDetail d : booking.getOrderDetails()) {
                if (d.getTotalPrice() != null) {
                    subTotal = subTotal.add(d.getTotalPrice());
                }
            }
        }

        booking.setSubTotal(subTotal);

        BigDecimal currentDiscount = booking.getDiscountAmount() != null
                ? booking.getDiscountAmount()
                : BigDecimal.ZERO;

        BigDecimal finalTotal = subTotal.subtract(currentDiscount);

        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        booking.setTotalAmount(finalTotal);
    }

    @Transactional
    public AdminBookingDetailResponse moveBookingTable(Long bookingId, Long newTableId) {
        RestaurantBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn đặt bàn không tồn tại"));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển bàn cho đơn đã hoàn thành hoặc bị hủy.");
        }

        RestaurantTable oldTable = booking.getTable();

        if (oldTable != null && oldTable.getTableId().equals(newTableId)) {
            return mapToAdminDetailResponse(booking);
        }

        RestaurantTable newTable = tableRepository.findById(newTableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn mới không tồn tại"));

        if (booking.getStatus() == BookingStatus.SERVING) {
            if (newTable.getStatus() != TableStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bàn " + newTable.getTableNumber() + " đang có khách hoặc đã được đặt, không thể chuyển đến.");
            }

            if (oldTable != null) {
                oldTable.setStatus(TableStatus.AVAILABLE);
                tableRepository.save(oldTable);
            }

            newTable.setStatus(TableStatus.SERVING);
            tableRepository.save(newTable);
        }

        else {
            LocalDateTime start = booking.getBookingTime();
            LocalDateTime end = booking.getEndTime();

            boolean isOccupied = bookingRepository.existsByTableAndDateRangeExcludingBooking(
                    newTableId, start, end, bookingId,
                    List.of(BookingStatus.CONFIRMED, BookingStatus.SERVING, BookingStatus.PENDING));

            if (isOccupied) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bàn " + newTable.getTableNumber() + " đã có lịch đặt khác trong khung giờ này.");
            }

        }

        booking.setTable(newTable);
        RestaurantBooking savedBooking = bookingRepository.save(booking);

        if (oldTable != null) {
            sseService.sendNotification("RESTAURANT_TABLE_UPDATE", oldTable.getTableId());
        }
        sseService.sendNotification("RESTAURANT_TABLE_UPDATE", newTable.getTableId());
        sseService.sendNotification("BOOKING_UPDATE", savedBooking.getBookingId());

        return mapToAdminDetailResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public RestaurantStatisticResponse getRevenueStatistics() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = now.toLocalDate().atTime(LocalTime.MAX);

        LocalDateTime startOfWeek = now.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = now.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                .atTime(LocalTime.MAX);

        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                .atTime(LocalTime.MAX);

        BigDecimal revenueToday = bookingRepository.sumRevenueByTimeRange(startOfToday, endOfToday);
        long countToday = bookingRepository.countCompletedByTimeRange(startOfToday, endOfToday);

        BigDecimal revenueWeek = bookingRepository.sumRevenueByTimeRange(startOfWeek, endOfWeek);
        long countWeek = bookingRepository.countCompletedByTimeRange(startOfWeek, endOfWeek);

        BigDecimal revenueMonth = bookingRepository.sumRevenueByTimeRange(startOfMonth, endOfMonth);
        long countMonth = bookingRepository.countCompletedByTimeRange(startOfMonth, endOfMonth);

        BigDecimal revenueTotal = bookingRepository.sumTotalRevenue();
        long countTotal = bookingRepository.countTotalCompleted();

        return RestaurantStatisticResponse.builder()
                .today(RestaurantStatisticResponse.MetricDetail.builder()
                        .revenue(revenueToday)
                        .completedOrders(countToday)
                        .build())
                .thisWeek(RestaurantStatisticResponse.MetricDetail.builder()
                        .revenue(revenueWeek)
                        .completedOrders(countWeek)
                        .build())
                .thisMonth(RestaurantStatisticResponse.MetricDetail.builder()
                        .revenue(revenueMonth)
                        .completedOrders(countMonth)
                        .build())
                .total(RestaurantStatisticResponse.MetricDetail.builder()
                        .revenue(revenueTotal)
                        .completedOrders(countTotal)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChartDataResponse> getRevenueChart(String type) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        List<Object[]> rawData;

        switch (type) {
            case "this_month":
                startTime = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                rawData = bookingRepository.getDailyRevenueChartData(startTime, endTime);
                break;

            case "monthly":
                startTime = LocalDate.now().withDayOfYear(1).atStartOfDay();
                rawData = bookingRepository.getMonthlyRevenueChartData(startTime, endTime);
                break;

            case "this_week":
            default:
                startTime = LocalDate.now()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        .atStartOfDay();

                rawData = bookingRepository.getDailyRevenueChartData(startTime, endTime);
                break;
        }

        return rawData.stream()
                .map(row -> new ChartDataResponse(
                        (String) row[0],
                        (row[1] != null) ? (BigDecimal) row[1] : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}
