package com.tgb.cp_dns.entity.homestay;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.HomestayCommonStatus;

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

    @Enumerated(EnumType.STRING)
    private HomestayCommonStatus status;
    @JsonIgnore
    private Boolean isDeleted = false;
}
