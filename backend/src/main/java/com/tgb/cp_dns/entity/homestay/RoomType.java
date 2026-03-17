package com.tgb.cp_dns.entity.homestay;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tgb.cp_dns.enums.HomestayCommonStatus;

@Entity
@Data
@Table(name = "room_types")
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeId;

    @ManyToOne
    @JoinColumn(name = "class_id")
    @JsonIgnoreProperties("roomTypes")
    private HomestayRoomClass roomClass;

    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    private BigDecimal basePrice;
    private Integer maxAdults;
    private Integer maxChildren;

    @Enumerated(EnumType.STRING)
    private HomestayCommonStatus status;
    
    @JsonIgnore
    private Boolean isDeleted = false;

    @ManyToMany
    @JoinTable(name = "room_type_amenities", joinColumns = @JoinColumn(name = "type_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<HomestayAmenity> amenities;

    @OneToMany(mappedBy = "roomType")
    @OrderBy("displayOrder ASC")
    private List<RoomTypeImage> images;
}
