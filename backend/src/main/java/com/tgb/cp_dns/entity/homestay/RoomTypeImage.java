package com.tgb.cp_dns.entity.homestay;

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
    private RoomType roomType;
    
    private String imageUrl;
    private Boolean isThumbnail;
}