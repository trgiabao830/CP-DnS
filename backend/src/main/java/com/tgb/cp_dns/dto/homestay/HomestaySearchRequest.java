package com.tgb.cp_dns.dto.homestay;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HomestaySearchRequest {
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
}