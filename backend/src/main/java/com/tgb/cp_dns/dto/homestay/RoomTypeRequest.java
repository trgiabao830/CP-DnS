package com.tgb.cp_dns.dto.homestay;

import com.tgb.cp_dns.enums.HomestayCommonStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class RoomTypeRequest {
    @NotBlank(message = "Tên loại phòng không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá cơ bản không được để trống")
    @Min(value = 0, message = "Giá cơ bản không được âm")
    private BigDecimal basePrice;

    @NotNull(message = "Số lượng người lớn tối đa không được để trống")
    @Min(value = 1, message = "Phải có ít nhất 1 người lớn")
    private Integer maxAdults;

    @NotNull(message = "Số lượng trẻ em tối đa không được để trống")
    @Min(value = 0, message = "Số lượng trẻ em không được âm")
    private Integer maxChildren;

    @NotNull(message = "Vui lòng chọn hạng phòng (Class ID)")
    private Long classId;

    private Set<Long> amenityIds;

    @NotNull(message = "Trạng thái không được để trống")
    private HomestayCommonStatus status;

    private List<Long> keptImageIds;
}
