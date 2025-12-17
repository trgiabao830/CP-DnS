package com.tgb.cp_dns.dto.restaurant;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FoodResponse {
    private Long foodId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private String imageUrl;
    private String status;
    private String categoryName;
    private List<VariantDto> variants;

    @Data
    @Builder
    public static class VariantDto {
        private Long variantId;
        private String name;
        private Boolean isRequired;
        private List<OptionDto> options;
    }

    @Data
    @Builder
    public static class OptionDto {
        private Long optionId;
        private String name;
        private BigDecimal priceAdjustment;
        private String status;
        private Long linkedFoodId;
        private List<VariantDto> linkedVariants;
    }
}
