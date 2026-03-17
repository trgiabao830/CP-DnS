package com.tgb.cp_dns.repository.auth;

import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.enums.UserStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneAndIsDeletedFalse(String phone);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByUserIdAndIsDeletedFalse(Long userId);

    boolean existsByPhoneAndIsDeletedFalse(String phone);

    boolean existsByEmailAndIsDeletedFalse(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "u.isDeleted = false AND " +
            "(:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword OR u.phone LIKE :keyword) AND "
            +
            "(:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable);
}