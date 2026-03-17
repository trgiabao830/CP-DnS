package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.SupportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {

    Optional<SupportSession> findByGuestSessionId(String guestSessionId);

    List<SupportSession> findAllByStatusOrderByCreatedAtDesc(String status);
    
    List<SupportSession> findAllByStatusOrderByCreatedAtAsc(String status);

    List<SupportSession> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}