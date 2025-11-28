package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "homestay_amenities")
public class HomestayAmenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long amenityId;
    private String name;
    private String iconUrl;
}
