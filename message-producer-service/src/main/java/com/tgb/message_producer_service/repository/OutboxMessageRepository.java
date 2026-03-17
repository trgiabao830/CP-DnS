package com.tgb.message_producer_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tgb.message_producer_service.model.OutboxMessage;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE OutboxMessage o SET o.sent = true, o.sentAt = :sentAt WHERE o.payload = :txnRef")
    void markAsSent(String txnRef, LocalDateTime sentAt);

    List<OutboxMessage> findAllBySentFalse();
}
