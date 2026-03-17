package com.tgb.cp_dns.enums;

public enum BookingStatus {
    // Trạng thái chung
    PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW,
    // Trạng thái cụ thể cho đặt bàn
    SERVING, 
    // Trạng thái cụ thể cho đặt phòng
    CHECKED_IN
}
