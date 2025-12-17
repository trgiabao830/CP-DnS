package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class VariantRequest {
    private Long variantId;

    @NotBlank(message = "Tên nhóm tùy chọn không được để trống")
    private String name;

    @NotNull
    private Boolean isRequired;

    private List<VariantOptionRequest> options = new ArrayList<>();
}