package com.tgb.cp_dns.entity.homestay;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "room_type_images")
public class RoomTypeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne
    @JoinColumn(name = "type_id")
    @JsonIgnore
    private RoomType roomType;

    private String imageUrl;

    private Integer displayOrder;
}