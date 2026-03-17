package com.tgb.cp_dns.service;

import com.tgb.cp_dns.dto.common.ChatMessageDto;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.auth.User;
import com.tgb.cp_dns.entity.common.SupportMessage;
import com.tgb.cp_dns.entity.common.SupportSession;
import com.tgb.cp_dns.entity.log.SupportLog;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.auth.UserRepository;
import com.tgb.cp_dns.repository.common.SupportLogRepository;
import com.tgb.cp_dns.repository.common.SupportMessageRepository;
import com.tgb.cp_dns.repository.common.SupportSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SupportSessionRepository sessionRepo;
    private final SupportMessageRepository messageRepo;
    private final SupportLogRepository logRepo;
    private final UserRepository userRepo;
    private final EmployeeRepository employeeRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void handleIncomingMessage(ChatMessageDto dto) {
        SupportSession session = getOrCreateSession(dto);

        SupportMessage msg = new SupportMessage();
        msg.setSession(session);
        msg.setMessage(dto.getContent());
        msg.setSenderType(dto.getSenderType());
        msg.setCreatedAt(LocalDateTime.now());

        if ("EMPLOYEE".equals(dto.getSenderType())) {
            if (dto.getUserId() != null) {
                Employee emp = employeeRepo.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("Employee not found"));
                msg.setSenderId(emp.getEmpId());
                msg.setSenderName(emp.getFullName());

                dto.setSenderName(emp.getFullName());
            } else {
                msg.setSenderName("Nhân viên hỗ trợ");
            }
        } else {
            msg.setSenderId(dto.getUserId());
            msg.setSenderName(session.getGuestName());

            dto.setSenderName(session.getGuestName());
        }

        SupportMessage savedMsg = messageRepo.save(msg);

        dto.setMsgId(savedMsg.getMsgId());

        dto.setCreatedAt(savedMsg.getCreatedAt());

        messagingTemplate.convertAndSend("/topic/chat/" + session.getGuestSessionId(), dto);

        if ("GUEST".equals(dto.getSenderType())) {
            messagingTemplate.convertAndSend("/topic/admin/sessions", "UPDATE_LIST");
        }
    }

    private SupportSession getOrCreateSession(ChatMessageDto dto) {
        return sessionRepo.findByGuestSessionId(dto.getGuestSessionId())
                .map(existingSession -> {
                    boolean isUpdated = false;

                    if ("GUEST".equals(dto.getSenderType()) &&
                            "COMPLETED".equals(existingSession.getStatus())) {
                        existingSession.setStatus("PENDING");
                        isUpdated = true;
                    }

                    if (existingSession.getGuestName() == null && dto.getSenderName() != null) {
                        existingSession.setGuestName(dto.getSenderName());
                        isUpdated = true;
                    }
                    if (existingSession.getGuestPhone() == null && dto.getGuestPhone() != null) {
                        existingSession.setGuestPhone(dto.getGuestPhone());
                        isUpdated = true;
                    }
                    if (existingSession.getGuestEmail() == null && dto.getGuestEmail() != null) {
                        existingSession.setGuestEmail(dto.getGuestEmail());
                        isUpdated = true;
                    }

                    if (existingSession.getUser() == null && dto.getUserId() != null) {
                        userRepo.findById(dto.getUserId()).ifPresent(user -> {
                            existingSession.setUser(user);
                        });
                        isUpdated = true;
                    }

                    if (isUpdated) {
                        return sessionRepo.save(existingSession);
                    }
                    return existingSession;
                })
                .orElseGet(() -> {
                    SupportSession newSession = new SupportSession();
                    newSession.setGuestSessionId(dto.getGuestSessionId());
                    newSession.setStatus("PENDING");

                    newSession.setGuestName(dto.getSenderName() != null ? dto.getSenderName() : "Khách ẩn danh");
                    newSession.setGuestPhone(dto.getGuestPhone());
                    newSession.setGuestEmail(dto.getGuestEmail());

                    if (dto.getUserId() != null) {
                        User user = userRepo.findById(dto.getUserId()).orElse(null);
                        newSession.setUser(user);

                        if (user != null && dto.getSenderName() == null) {
                            newSession.setGuestName(user.getFullName());
                            newSession.setGuestPhone(user.getPhone());
                            newSession.setGuestEmail(user.getEmail());
                        }
                    }

                    return sessionRepo.save(newSession);
                });
    }

    @Transactional
    public SupportSession handleEmployeeAction(Long sessionId, String action, Long employeeId) {
        SupportSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        SupportLog log = new SupportLog();
        log.setSession(session);
        log.setEmployee(employee);

        if ("ACCEPT".equalsIgnoreCase(action)) {
            session.setStatus("SERVING");
            log.setAction("ACCEPTED");
            log.setDescription("Nhân viên " + employee.getFullName() + " đã tiếp nhận hội thoại.");
        } else if ("COMPLETE".equalsIgnoreCase(action)) {
            session.setStatus("COMPLETED");
            log.setAction("COMPLETED");
            log.setDescription("Hội thoại đã kết thúc.");
        }

        logRepo.save(log);

        SupportSession savedSession = sessionRepo.save(session);

        ChatMessageDto systemMsg = new ChatMessageDto();
        systemMsg.setSenderType("SYSTEM");
        systemMsg.setContent(log.getDescription());
        systemMsg.setGuestSessionId(session.getGuestSessionId());

        systemMsg.setCreatedAt(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/chat/" + session.getGuestSessionId(), systemMsg);

        messagingTemplate.convertAndSend("/topic/admin/sessions", "UPDATE_LIST");

        return savedSession;
    }
}