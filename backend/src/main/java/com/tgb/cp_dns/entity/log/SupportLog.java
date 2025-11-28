package com.tgb.cp_dns.entity.log;

import com.tgb.cp_dns.entity.common.SupportSession;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "support_logs")
public class SupportLog extends BaseLog {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SupportSession session;

    @Column(name = "action")
    private String action;
}
