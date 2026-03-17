package com.tgb.cp_dns.repository.auth;

import com.tgb.cp_dns.entity.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Set<Permission> findAllByPermissionIdIn(Set<Long> permissionIds);

    List<Permission> findByDescriptionContainingIgnoreCase(String description);
}
