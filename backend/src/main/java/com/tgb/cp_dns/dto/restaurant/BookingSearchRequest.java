package com.tgb.cp_dns.dto.restaurant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingSearchRequest {
    @NotNull(message = "Ngày đặt không được để trống")
    private LocalDate date;

    @NotNull(message = "Giờ đặt không được để trống")
    private LocalTime time;

    @NotNull(message = "Số lượng khách không được để trống")
    private Integer numberOfGuests;
}
