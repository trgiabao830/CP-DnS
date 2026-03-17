package com.tgb.cp_dns.entity.homestay;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgb.cp_dns.enums.RoomStatus;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private RoomType roomType;

    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;
    @JsonIgnore
    private Boolean isDeleted = false;
}
