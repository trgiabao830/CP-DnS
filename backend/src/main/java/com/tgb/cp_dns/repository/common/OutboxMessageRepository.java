package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.common.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE OutboxMessage o SET o.sent = true, o.sentAt = :sentAt, o.processed = true WHERE o.payload = :txnRef")
    void markAsSentAndProcessed(String txnRef, LocalDateTime sentAt);
}
