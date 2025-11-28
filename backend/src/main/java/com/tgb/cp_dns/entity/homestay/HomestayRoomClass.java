package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "homestay_room_classes")
public class HomestayRoomClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long classId;
    private String name;
}