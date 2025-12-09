package com.tgb.cp_dns.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String toEmail, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Yêu cầu đặt lại mật khẩu - Cây Phượng - Dine & Stay");
        message.setText("Bạn đã gửi yêu cầu đặt lại mật khẩu.\n"
                + "Vui lòng nhấp vào đường dẫn sau để đổi mật khẩu (hết hạn sau 5 phút):\n\n"
                + resetUrl + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.");
        
        mailSender.send(message);
    }
}