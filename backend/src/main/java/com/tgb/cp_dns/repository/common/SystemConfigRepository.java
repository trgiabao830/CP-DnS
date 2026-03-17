package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
