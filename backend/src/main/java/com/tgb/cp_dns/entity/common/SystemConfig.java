package com.tgb.cp_dns.entity.common;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "system_configs")
public class SystemConfig {
    @Id
    private String configKey;

    private String configValue;
    
    private String description;
}
