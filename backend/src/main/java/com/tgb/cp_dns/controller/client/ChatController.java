package com.tgb.cp_dns.controller.client;

import com.tgb.cp_dns.dto.common.ChatMessageDto;
import com.tgb.cp_dns.entity.auth.Employee;
import com.tgb.cp_dns.repository.auth.EmployeeRepository;
import com.tgb.cp_dns.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final EmployeeRepository employeeRepository;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage, Principal principal) {

        if ("EMPLOYEE".equals(chatMessage.getSenderType())) {

            if (principal == null) {
                log.warn("Cảnh báo: Nhận tin nhắn EMPLOYEE từ nguồn chưa xác thực (Session null).");
                return;
            }

            String username = principal.getName();
            Employee employee = employeeRepository.findByUsernameAndIsDeletedFalse(username)
                    .orElse(null);

            if (employee != null) {
                chatMessage.setUserId(employee.getEmpId());
                chatMessage.setSenderName(employee.getFullName());

                log.info("Nhân viên gửi tin: {} (ID: {})", employee.getFullName(), employee.getEmpId());
            } else {
                log.error("Lỗi bảo mật: User '{}' cố tình gửi tin nhắn dưới danh nghĩa EMPLOYEE.", username);
                return;
            }
        }

        chatService.handleIncomingMessage(chatMessage);
    }
}