package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "room_types")
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeId;
    
    @ManyToOne
    @JoinColumn(name = "class_id")
    private HomestayRoomClass roomClass;

    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer maxAdults;
    private Integer maxChildren;

    @ManyToMany
    @JoinTable(
        name = "room_type_amenities",
        joinColumns = @JoinColumn(name = "type_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<HomestayAmenity> amenities;
    
    @OneToMany(mappedBy = "roomType")
    private List<RoomTypeImage> images;
}
