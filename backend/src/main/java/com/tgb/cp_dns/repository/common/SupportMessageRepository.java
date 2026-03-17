package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findBySession_SessionIdOrderByCreatedAtAsc(Long sessionId);
}
