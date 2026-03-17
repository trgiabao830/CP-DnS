package com.tgb.cp_dns.repository.common;

import com.tgb.cp_dns.entity.log.SupportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportLogRepository extends JpaRepository<SupportLog, Long> {

    List<SupportLog> findBySession_SessionIdOrderByCreatedAtDesc(Long sessionId);

    List<SupportLog> findByEmployee_EmpId(Long empId);
}
