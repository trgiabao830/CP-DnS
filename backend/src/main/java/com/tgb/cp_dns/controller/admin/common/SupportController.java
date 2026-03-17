package com.tgb.cp_dns.controller.admin.common;

import com.tgb.cp_dns.dto.common.SessionActionRequest;
import com.tgb.cp_dns.dto.common.SupportMessageResponse;
import com.tgb.cp_dns.dto.common.SupportSessionResponse;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.entity.common.SupportMessage;
import com.tgb.cp_dns.entity.common.SupportSession;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.repository.common.SupportMessageRepository;
import com.tgb.cp_dns.repository.common.SupportSessionRepository;
import com.tgb.cp_dns.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class SupportController {

    private final ChatService chatService;

    private final SupportSessionRepository sessionRepository;
    private final SupportMessageRepository messageRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('SUPPORT_VIEW')")
    public ResponseEntity<List<SupportSessionResponse>> getAllSessions(@RequestParam(required = false) String status) {
        List<SupportSession> sessions;

        if (status != null && !status.isEmpty()) {
            sessions = sessionRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            sessions = sessionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<SupportSessionResponse> response = sessions.stream().map(session -> {
            SupportSessionResponse dto = new SupportSessionResponse();
            dto.setSessionId(session.getSessionId());
            dto.setGuestSessionId(session.getGuestSessionId());

            dto.setGuestName(session.getGuestName());
            dto.setGuestPhone(session.getGuestPhone());
            dto.setGuestEmail(session.getGuestEmail());

            dto.setStatus(session.getStatus());
            dto.setCreatedAt(session.getCreatedAt());

            if (session.getUser() != null) {
                dto.setUserId(session.getUser().getUserId());
            }

            return dto;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAuthority('SUPPORT_VIEW')")
    public ResponseEntity<List<SupportMessageResponse>> getSessionMessages(@PathVariable Long sessionId) {

        List<SupportMessage> messages = messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);

        List<SupportMessageResponse> response = messages.stream().map(msg -> {
            SupportMessageResponse dto = new SupportMessageResponse();
            dto.setMsgId(msg.getMsgId());
            dto.setMessage(msg.getMessage());
            dto.setSenderType(msg.getSenderType());
            dto.setSenderId(msg.getSenderId());
            dto.setSenderName(msg.getSenderName());
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/action")
    @PreAuthorize("hasAuthority('SUPPORT_ACTION')")
    public ResponseEntity<SupportSessionResponse> performAction(@RequestBody SessionActionRequest request,
            Authentication authentication) {

        String currentUsername = authentication.getName();

        Employee employee = employeeRepository.findByUsernameAndIsDeletedFalse(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy thông tin nhân viên."));

        SupportSession updatedSession = chatService.handleEmployeeAction(request.getSessionId(), request.getAction(),
                employee.getEmpId());

        SupportSessionResponse response = new SupportSessionResponse();
        response.setSessionId(updatedSession.getSessionId());
        response.setGuestSessionId(updatedSession.getGuestSessionId());
        response.setGuestName(updatedSession.getGuestName());
        response.setGuestPhone(updatedSession.getGuestPhone());
        response.setGuestEmail(updatedSession.getGuestEmail());
        response.setStatus(updatedSession.getStatus());
        response.setCreatedAt(updatedSession.getCreatedAt());

        if (updatedSession.getUser() != null) {
            response.setUserId(updatedSession.getUser().getUserId());
        }

        return ResponseEntity.ok(response);
    }
}