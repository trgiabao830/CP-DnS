package com.tgb.cp_dns.repository.auth;

import com.tgb.cp_dns.entity.auth.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
}