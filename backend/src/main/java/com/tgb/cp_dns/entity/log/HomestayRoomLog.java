package com.tgb.cp_dns.entity.log;

import com.tgb.cp_dns.entity.homestay.Room;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "homestay_room_logs")
public class HomestayRoomLog extends BaseLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "action_type")
    private String actionType;
}
