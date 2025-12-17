package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReorderItem {
    @NotNull
    private Long id;

    @NotNull
    private Integer order;
}
