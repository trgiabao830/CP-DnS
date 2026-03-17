package com.tgb.cp_dns.service;

import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendBaseUrl;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final String SEPARATOR = "--------------------------------------------------";

    @Async
    public void sendResetPasswordEmail(String toEmail, String resetUrl) {
        String subject = "Yêu cầu đặt lại mật khẩu - Cây Phượng - Dine & Stay";

        StringBuilder content = new StringBuilder();
        content.append("Xin chào,\n\n");
        content.append("Bạn đã gửi yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n");
        content.append(
                "Vui lòng nhấp vào đường dẫn bên dưới để thiết lập mật khẩu mới (Liên kết hết hạn sau 5 phút):\n\n");
        content.append(resetUrl).append("\n\n");
        content.append("Lưu ý: Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.\n");
        content.append(getEmailFooter());

        sendEmail(toEmail, subject, content.toString());
    }

    @Async
    public void sendBookingCreatedEmail(String toEmail, Object booking) {
        String subject = "";
        String customerName = "";
        String detailLink = "";
        BigDecimal depositAmount = BigDecimal.ZERO;

        if (booking instanceof RestaurantBooking rb) {
            subject = "Thông báo ghi nhận đặt bàn #" + rb.getBookingId();
            customerName = rb.getCustomerName();
            depositAmount = rb.getDepositAmount();
            detailLink = frontendBaseUrl + "/restaurant/booking/tracking?code=" + rb.getAccessToken();
        } else if (booking instanceof HomestayBooking hb) {
            subject = "Thông báo ghi nhận đặt phòng #" + hb.getBookingId();
            customerName = hb.getCustomerName();
            depositAmount = hb.getDepositAmount();
            detailLink = frontendBaseUrl + "/homestay/booking/tracking?code=" + hb.getAccessToken();
        }

        StringBuilder content = new StringBuilder();
        content.append("Cảm ơn ").append(customerName).append(",\n\n");
        content.append("Yêu cầu của bạn đã được ghi nhận thành công.\n");
        content.append(SEPARATOR).append("\n");
        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            content.append("• Tiền cọc cần thanh toán: ").append(formatCurrency(depositAmount)).append("\n");
        }
        content.append(SEPARATOR).append("\n\n");
        content.append("Bạn có thể theo dõi đơn tại: ").append(detailLink);
        content.append(getEmailFooter());

        sendEmail(toEmail, subject, content.toString());
    }

    @Async
    public void sendPaymentRequestEmail(String toEmail, Object booking, String paymentUrl) {
        String subject = "";
        String customerName = "";
        String detailLink = "";
        BigDecimal depositAmount = BigDecimal.ZERO;
        String typeLabel = "";

        if (booking instanceof RestaurantBooking rb) {
            subject = "Yêu cầu thanh toán đặt bàn #" + rb.getBookingId();
            customerName = rb.getCustomerName();
            depositAmount = rb.getDepositAmount();
            typeLabel = "đặt bàn nhà hàng";
            detailLink = frontendBaseUrl + "/restaurant/booking/tracking?code=" + rb.getAccessToken();
        } else if (booking instanceof HomestayBooking hb) {
            subject = "Yêu cầu thanh toán đặt phòng #" + hb.getBookingId();
            customerName = hb.getCustomerName();
            depositAmount = hb.getDepositAmount();
            typeLabel = "đặt phòng Homestay";
            detailLink = frontendBaseUrl + "/homestay/booking/tracking?code=" + hb.getAccessToken();
        }

        StringBuilder content = new StringBuilder();
        content.append("Chào ").append(customerName).append(",\n\n");
        content.append("Nhân viên vừa hỗ trợ bạn tạo đơn ").append(typeLabel).append(".\n");
        content.append("Vui lòng thanh toán cọc để hoàn tất đơn:\n");
        content.append(SEPARATOR).append("\n");
        content.append("• Tiền cọc: ").append(formatCurrency(depositAmount)).append("\n");
        content.append(SEPARATOR).append("\n\n");
        content.append(">>> Nhấp vào đây để thanh toán: <<<\n");
        content.append(paymentUrl).append("\n\n");
        content.append("Xem chi tiết đơn tại: ").append(detailLink);
        content.append(getEmailFooter());

        sendEmail(toEmail, subject, content.toString());
    }

    @Async
    public void sendBookingSuccessEmail(String toEmail, Object booking) {
        String subject = "";
        String customerName = "";
        String detailLink = "";
        LocalDateTime timeDisplay = null;
        String typeLabel = "";

        if (booking instanceof RestaurantBooking rb) {
            subject = "Xác nhận đặt bàn thành công #" + rb.getBookingId();
            customerName = rb.getCustomerName();
            timeDisplay = rb.getBookingTime();
            typeLabel = "Bàn ăn";
            detailLink = frontendBaseUrl + "/restaurant/booking/tracking?code=" + rb.getAccessToken();
        } else if (booking instanceof HomestayBooking hb) {
            subject = "Xác nhận đặt phòng thành công #" + hb.getBookingId();
            customerName = hb.getCustomerName();
            timeDisplay = hb.getCheckInDate().atStartOfDay();
            typeLabel = "Phòng";
            detailLink = frontendBaseUrl + "/homestay/booking/tracking?code=" + hb.getAccessToken();
        }

        StringBuilder content = new StringBuilder();
        content.append("Chào ").append(customerName).append(",\n\n");
        content.append("Đơn đặt ").append(typeLabel).append(" của bạn đã được xác nhận thành công.\n");
        content.append(SEPARATOR).append("\n");
        content.append("• Thời gian: ").append(formatDateTime(timeDisplay)).append("\n");
        content.append(SEPARATOR).append("\n\n");
        content.append("Xem chi tiết đơn của bạn tại: ").append(detailLink);
        content.append(getEmailFooter());

        sendEmail(toEmail, subject, content.toString());
    }

    @Async
    public void sendBookingCancellationEmail(String toEmail, Object booking) {
        String subject = "";
        String customerName = "";
        String typeLabel = "";
        Long bookingId = null;

        if (booking instanceof RestaurantBooking rb) {
            bookingId = rb.getBookingId();
            customerName = rb.getCustomerName();
            typeLabel = "đặt bàn";
            subject = "Thông báo hủy đặt bàn #" + bookingId;

        } else if (booking instanceof HomestayBooking hb) {
            bookingId = hb.getBookingId();
            customerName = hb.getCustomerName();
            typeLabel = "đặt phòng";
            subject = "Thông báo hủy đặt phòng #" + bookingId;
        }

        StringBuilder content = new StringBuilder();
        content.append("Chào ").append(customerName).append(",\n\n");

        content.append("Đơn ").append(typeLabel).append(" mã #").append(bookingId)
                .append(" của bạn đã được HỦY thành công.\n\n");

        content.append("Lưu ý về hoàn tiền:\n");
        content.append(
                "- Nếu bạn đã thanh toán cọc và thỏa mãn chính sách hoàn hủy (trước 48h), hệ thống sẽ xử lý hoàn tiền trong vòng 48 giờ làm việc.\n");
        content.append("- Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ hotline để được hỗ trợ.\n");

        content.append(getEmailFooter());

        sendEmail(toEmail, subject, content.toString());
    }

    private void sendEmail(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    private String getEmailFooter() {
        return "\n\n" + SEPARATOR + "\n"
                + "Cây Phượng - Dine & Stay\n"
                + "Hotline: 0123123123\n"
                + "Địa chỉ: TP. HCM, Việt Nam\n"
                + "Website: " + frontendBaseUrl;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "Chưa cập nhật";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0 đ";
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(amount);
    }
}