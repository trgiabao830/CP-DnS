package com.tgb.cp_dns.repository.auth;

import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.enums.EmployeeStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
        Optional<Employee> findByUsername(String username);

        Optional<Employee> findByUsernameAndIsDeletedFalse(String username);

        boolean existsByUsernameAndIsDeletedFalse(String username);

        boolean existsByPhoneAndIsDeletedFalse(String phone);

        @Query("SELECT e FROM Employee e WHERE " +
                        "e.isDeleted = false AND " +
                        "(:keyword IS NULL OR LOWER(e.fullName) LIKE :keyword) AND " +
                        "(:status IS NULL OR e.status = :status)")
        Page<Employee> searchEmployees(
                        @Param("keyword") String keyword,
                        @Param("status") EmployeeStatus status,
                        Pageable pageable);

        Optional<Employee> findByEmpIdAndIsDeletedFalse(Long empId);
}