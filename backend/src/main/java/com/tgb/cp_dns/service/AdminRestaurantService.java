package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.restaurant.*;
import com.tgb.cp_dns.entity.restaurant.*;
import com.tgb.cp_dns.enums.AreaStatus;
import com.tgb.cp_dns.enums.CategoryStatus;
import com.tgb.cp_dns.enums.FoodStatus;
import com.tgb.cp_dns.enums.OptionStatus;
import com.tgb.cp_dns.enums.TableStatus;
import com.tgb.cp_dns.repository.restaurant.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminRestaurantService {

    private final FoodCategoryRepository categoryRepository;
    private final FoodRepository foodRepository;
    private final RestaurantAreaRepository areaRepository;
    private final RestaurantTableRepository tableRepository;

    @Transactional(readOnly = true)
    public List<FoodCategory> getAllCategories() {
        return categoryRepository.findAllByIsDeletedFalseOrderByDisplayOrderAsc();
    }

    @Transactional
    public FoodCategory createCategory(String name) {
        if (categoryRepository.existsByNameAndIsDeletedFalse(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại.");
        }

        FoodCategory category = new FoodCategory();
        category.setName(name);
        Integer nextOrder = categoryRepository.findMaxDisplayOrder() + 1;
        category.setDisplayOrder(nextOrder);
        category.setStatus(CategoryStatus.AVAILABLE);
        return categoryRepository.save(category);
    }

    @Transactional
    public void updateCategory(Long id, CategoryRequest request) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        if (categoryRepository.existsByNameAndCategoryIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại.");
        }

        category.setName(request.getName());
        categoryRepository.save(category);
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
    }

    @Transactional
    public void updateCategoryStatus(Long id, CategoryStatus status) {
        FoodCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));

        category.setStatus(status);
        categoryRepository.save(category);
    }

    @Transactional
    public void reorderCategories(List<ReorderItem> items) {
        for (ReorderItem item : items) {
            FoodCategory category = categoryRepository.findById(item.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Danh mục ID " + item.getId() + " không tồn tại"));
            category.setDisplayOrder(item.getOrder());
            categoryRepository.save(category);
        }
    }

    @Transactional(readOnly = true)
    public Page<RestaurantArea> getAllAreas(Pageable pageable) {
        return areaRepository.findAllByIsDeletedFalse(pageable);
    }

    @Transactional
    public void createArea(RestaurantAreaRequest request) {
        if (areaRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên khu vực đã tồn tại.");
        }

        RestaurantArea area = new RestaurantArea();
        area.setName(request.getName());
        area.setStatus(AreaStatus.AVAILABLE);
        areaRepository.save(area);
    }

    @Transactional
    public void updateArea(Long id, RestaurantAreaRequest request) {
        RestaurantArea area = areaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        if (areaRepository.existsByNameAndAreaIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên khu vực đã tồn tại.");
        }

        area.setName(request.getName());
        areaRepository.save(area);
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
    }

    @Transactional
    public void updateAreaStatus(Long areaId, AreaStatus status) {
        RestaurantArea area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        area.setStatus(status);
        areaRepository.save(area);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantTable> getAllTables(Pageable pageable) {
        return tableRepository.findAllByIsDeletedFalse(pageable);
    }

    @Transactional
    public void createTable(RestaurantTableRequest request) {
        if (tableRepository.existsByTableNumberAndIsDeletedFalse(request.getTableNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số bàn đã tồn tại.");
        }

        RestaurantArea area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khu vực không tồn tại"));

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());
        table.setArea(area);
        table.setStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }

    @Transactional
    public void updateTable(Long id, RestaurantTableRequest request) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        if (tableRepository.existsByTableNumberAndTableIdNotAndIsDeletedFalse(request.getTableNumber(), id)) {
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
    }

    @Transactional
    public void deleteTable(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        table.setIsDeleted(true);
        table.setStatus(TableStatus.UNAVAILABLE);
        tableRepository.save(table);
    }

    @Transactional
    public void updateTableStatus(Long id, TableStatus status) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bàn ăn không tồn tại"));

        table.setStatus(status);
        tableRepository.save(table);
    }

    @Transactional(readOnly = true)
    public Page<FoodResponse> getAllFoods(Pageable pageable) {
        return foodRepository.findAllByIsDeletedFalse(pageable)
                .map(this::mapToFoodResponse);
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> getFoodsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại");
        }
        return foodRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByDisplayOrderAsc(categoryId)
                .stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createFood(FoodRequest request) {
        if (foodRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại.");
        }

        FoodCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));
        Integer nextOrder = foodRepository.findMaxDisplayOrder(category.getCategoryId()) + 1;

        Food food = new Food();
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setBasePrice(request.getBasePrice());
        food.setDiscountPrice(request.getDiscountPrice());
        food.setImageUrl(request.getImageUrl());
        food.setDisplayOrder(nextOrder);
        food.setStatus(FoodStatus.AVAILABLE);
        food.setCategory(category);
        mergeVariants(food, request.getVariants());
        foodRepository.save(food);
    }

    @Transactional
    public void updateFood(Long id, FoodRequest request) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        if (foodRepository.existsByNameAndFoodIdNotAndIsDeletedFalse(request.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên món ăn đã tồn tại.");
        }

        if (!food.getCategory().getCategoryId().equals(request.getCategoryId())) {
            FoodCategory newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục mới không tồn tại"));
            Integer nextOrder = foodRepository.findMaxDisplayOrder(newCategory.getCategoryId()) + 1;
            food.setCategory(newCategory);
            food.setDisplayOrder(nextOrder);
        }

        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setBasePrice(request.getBasePrice());
        food.setDiscountPrice(request.getDiscountPrice());
        food.setImageUrl(request.getImageUrl());
        mergeVariants(food, request.getVariants());
        foodRepository.save(food);
    }

    @Transactional
    public void updateFoodStatus(Long foodId, FoodStatus status) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        food.setStatus(status);
        foodRepository.save(food);
    }

    @Transactional
    public void deleteFood(Long id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Món ăn không tồn tại"));

        food.setIsDeleted(true);
        food.setStatus(FoodStatus.UNAVAILABLE);
        foodRepository.save(food);
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
                .categoryName(food.getCategory() != null ? food.getCategory().getName() : "")
                .variants(food.getVariants().stream()
                        .filter(v -> !v.getIsDeleted())
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
                        .map(this::mapToOptionDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private FoodResponse.OptionDto mapToOptionDto(FoodVariantOption option) {
        String finalStatus = option.getStatus().name();
        List<FoodResponse.VariantDto> childrenVariants = new ArrayList<>();

        if (option.getLinkedFood() != null) {
            Food linked = option.getLinkedFood();

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
}
