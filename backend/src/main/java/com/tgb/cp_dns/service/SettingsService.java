package com.tgb.cp_dns.service;

import com.tgb.cp_dns.entity.restaurant.Food;
import com.tgb.cp_dns.entity.common.SystemConfig;
import com.tgb.cp_dns.enums.FoodStatus;
import com.tgb.cp_dns.repository.restaurant.FoodRepository;
import com.tgb.cp_dns.repository.common.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemConfigRepository configRepository;
    private final FoodRepository foodRepository;
    private final SseNotificationService sseService;

    @Transactional(readOnly = true)
    public boolean getDepositRequirement() {
        return configRepository.findByConfigKey("BOOKING_REQUIRE_DEPOSIT_NO_FOOD")
                .map(cfg -> Boolean.parseBoolean(cfg.getConfigValue()))
                .orElse(true);
    }

    @Transactional
    public void updateDepositRequirement(boolean isRequired) {
        SystemConfig config = configRepository.findByConfigKey("BOOKING_REQUIRE_DEPOSIT_NO_FOOD")
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình hệ thống."));

        config.setConfigValue(String.valueOf(isRequired));
        configRepository.save(config);
    }

    @Transactional
    public int resetAllOutOfStockFoods() {
        List<Food> outOfStockFoods = foodRepository.findAllByStatusAndIsDeletedFalse(FoodStatus.OUT_OF_STOCK);

        if (outOfStockFoods.isEmpty()) {
            return 0;
        }

        for (Food food : outOfStockFoods) {
            food.setStatus(FoodStatus.AVAILABLE);
        }

        foodRepository.saveAll(outOfStockFoods);

        for (Food food : outOfStockFoods) {
            sseService.sendNotification("FOOD_UPDATE", food.getFoodId());
        }

        return outOfStockFoods.size();
    }

    @Transactional(readOnly = true)
    public int getHomestayCancellationDeadline() {
        return configRepository.findByConfigKey("HOMESTAY_CANCELLATION_DEADLINE_HOURS")
                .map(cfg -> Integer.parseInt(cfg.getConfigValue()))
                .orElse(48);
    }

    @Transactional(readOnly = true)
    public int getRestaurantCancellationDeadlineWithDeposit() {
        return configRepository.findByConfigKey("RESTAURANT_CANCELLATION_DEADLINE_HOURS_WITH_DEPOSIT")
                .map(cfg -> Integer.parseInt(cfg.getConfigValue()))
                .orElse(48);
    }

    @Transactional(readOnly = true)
    public int getRestaurantCancellationDeadlineNoDeposit() {
        return configRepository.findByConfigKey("RESTAURANT_CANCELLATION_DEADLINE_HOURS_NO_DEPOSIT")
                .map(cfg -> Integer.parseInt(cfg.getConfigValue()))
                .orElse(1);
    }


    @Transactional
    public void updateCancellationDeadlines(Integer homestayHours, Integer restDepositHours,
            Integer restNoDepositHours) {
        if (homestayHours != null) {
            updateOrCreateConfig("HOMESTAY_CANCELLATION_DEADLINE_HOURS", String.valueOf(homestayHours));
        }
        if (restDepositHours != null) {
            updateOrCreateConfig("RESTAURANT_CANCELLATION_DEADLINE_HOURS_WITH_DEPOSIT",
                    String.valueOf(restDepositHours));
        }
        if (restNoDepositHours != null) {
            updateOrCreateConfig("RESTAURANT_CANCELLATION_DEADLINE_HOURS_NO_DEPOSIT",
                    String.valueOf(restNoDepositHours));
        }
    }

    private void updateOrCreateConfig(String key, String value) {
        SystemConfig config = configRepository.findByConfigKey(key).orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription("Cấu hình tự động");
        configRepository.save(config);
    }

    @Transactional
    public void updateRestaurantOperatingHours(String openingTime, String closingTime) {

        LocalTime currentOpenTime = getRestaurantOpeningTime();
        LocalTime currentCloseTime = getRestaurantClosingTime();

        LocalTime newOpenTime = (openingTime != null && !openingTime.trim().isEmpty())
                ? LocalTime.parse(openingTime)
                : currentOpenTime;

        LocalTime newCloseTime = (closingTime != null && !closingTime.trim().isEmpty())
                ? LocalTime.parse(closingTime)
                : currentCloseTime;

        if (!newOpenTime.isBefore(newCloseTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ mở cửa phải trước giờ đóng cửa.");
        }

        if (openingTime != null && !openingTime.trim().isEmpty()) {
            SystemConfig openConfig = configRepository.findByConfigKey("RESTAURANT_OPENING_TIME")
                    .orElseGet(() -> {
                        SystemConfig newConfig = new SystemConfig();
                        newConfig.setConfigKey("RESTAURANT_OPENING_TIME");
                        return newConfig;
                    });
            openConfig.setConfigValue(openingTime);
            configRepository.save(openConfig);
        }

        if (closingTime != null && !closingTime.trim().isEmpty()) {
            SystemConfig closeConfig = configRepository.findByConfigKey("RESTAURANT_CLOSING_TIME")
                    .orElseGet(() -> {
                        SystemConfig newConfig = new SystemConfig();
                        newConfig.setConfigKey("RESTAURANT_CLOSING_TIME");
                        return newConfig;
                    });
            closeConfig.setConfigValue(closingTime);
            configRepository.save(closeConfig);
        }
    }

    @Transactional(readOnly = true)
    public LocalTime getRestaurantOpeningTime() {
        return configRepository.findByConfigKey("RESTAURANT_OPENING_TIME")
                .map(cfg -> LocalTime.parse(cfg.getConfigValue()))
                .orElse(LocalTime.of(8, 0));
    }

    @Transactional(readOnly = true)
    public LocalTime getRestaurantClosingTime() {
        return configRepository.findByConfigKey("RESTAURANT_CLOSING_TIME")
                .map(cfg -> LocalTime.parse(cfg.getConfigValue()))
                .orElse(LocalTime.of(21, 30));
    }
}