package com.tgb.cp_dns.service.queue;

import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.enums.PaymentMethod;
import com.tgb.cp_dns.repository.common.OutboxMessageRepository;
import com.tgb.cp_dns.repository.restaurant.RestaurantBookingRepository;
import com.tgb.cp_dns.repository.homestay.HomestayBookingRepository;
import com.tgb.cp_dns.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CancelBookingConsumer {

    private final RestaurantBookingRepository restaurantRepository;
    private final HomestayBookingRepository homestayRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final PaymentService paymentService;

    @RabbitListener(queues = "dlx.cancel.queue")
    public void handleCancelOrderMessage(String txnRef) {
        log.info("Checking payment status for txnRef: {}", txnRef);

        Optional<RestaurantBooking> resOpt = restaurantRepository.findByVnpTxnRef(txnRef);
        if (resOpt.isPresent()) {
            processBooking(resOpt.get(), txnRef);
            return;
        }

        Optional<HomestayBooking> homeOpt = homestayRepository.findByVnpTxnRef(txnRef);
        if (homeOpt.isPresent()) {
            processBooking(homeOpt.get(), txnRef);
            return;
        }

        log.warn("Booking not found in any table for txnRef: {}", txnRef);
        updateOutboxStatus(txnRef);
    }

    private void processBooking(Object bookingObj, String txnRef) {
        BookingStatus currentStatus;
        PaymentMethod paymentMethod;
        LocalDateTime createdAt;
        Long bookingId;

        if (bookingObj instanceof RestaurantBooking rb) {
            currentStatus = rb.getStatus();
            paymentMethod = rb.getPaymentMethod();
            createdAt = rb.getCreatedAt();
            bookingId = rb.getBookingId();
        } else {
            HomestayBooking hb = (HomestayBooking) bookingObj;
            currentStatus = hb.getStatus();
            paymentMethod = hb.getPaymentMethod();
            createdAt = hb.getCreatedAt();
            bookingId = hb.getBookingId();
        }

        if (currentStatus != BookingStatus.PENDING || paymentMethod != PaymentMethod.VNPAY) {
            log.info("Booking [{}] (txnRef: {}) skipped. Status: {}, Method: {}", 
                     bookingId, txnRef, currentStatus, paymentMethod);
            updateOutboxStatus(txnRef);
            return;
        }

        try {
            String txnDate = (createdAt != null ? createdAt : LocalDateTime.now().minusMinutes(15))
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            VNPayQueryResponse response = paymentService.queryTransaction(txnRef, txnDate);

            boolean isSuccess = "00".equals(response.getResponseCode()) && "00".equals(response.getTransactionStatus());
            
            if (isSuccess) {
                updateToConfirmed(bookingObj, response.getPayDate());
                log.info("Booking [{}] (txnRef: {}) updated to CONFIRMED via Query", bookingId, txnRef);
            } else {
                updateToCancelled(bookingObj);
                log.info("Booking [{}] (txnRef: {}) updated to CANCELLED", bookingId, txnRef);
            }

            updateOutboxStatus(txnRef);

        } catch (Exception e) {
            log.error("Error querying VNPay for txnRef {}: {}", txnRef, e.getMessage());
        }
    }

    private void updateToConfirmed(Object booking, String payDateStr) {
        LocalDateTime payTime = LocalDateTime.now();
        if (payDateStr != null && !payDateStr.isEmpty()) {
            try {
                payTime = LocalDateTime.parse(payDateStr, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            } catch (Exception ignored) {}
        }

        if (booking instanceof RestaurantBooking rb) {
            rb.setStatus(BookingStatus.CONFIRMED);
            rb.setPaymentTime(payTime);
            restaurantRepository.save(rb);
        } else if (booking instanceof HomestayBooking hb) {
            hb.setStatus(BookingStatus.CONFIRMED);
            hb.setPaymentTime(payTime);
            homestayRepository.save(hb);
        }
    }

    private void updateToCancelled(Object booking) {
        if (booking instanceof RestaurantBooking rb) {
            rb.setStatus(BookingStatus.CANCELLED);
            restaurantRepository.save(rb);
        } else if (booking instanceof HomestayBooking hb) {
            hb.setStatus(BookingStatus.CANCELLED);
            homestayRepository.save(hb);
        }
    }

    private void updateOutboxStatus(String txnRef) {
        try {
            outboxMessageRepository.markAsSentAndProcessed(txnRef, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to update OutboxMessage status", e);
        }
    }
}