package com.tgb.cp_dns.service;

import com.tgb.cp_dns.config.VNPayConfig;
import com.tgb.cp_dns.dto.payment.VNPayQueryResponse;
import com.tgb.cp_dns.dto.payment.VNPayRefundResponse;
import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;
import com.tgb.cp_dns.enums.BookingStatus;
import com.tgb.cp_dns.repository.homestay.HomestayBookingRepository;
import com.tgb.cp_dns.repository.restaurant.RestaurantBookingRepository;
import com.tgb.cp_dns.util.VNPayHashUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final RestaurantBookingRepository bookingRepository;
    private final HomestayBookingRepository homestayBookingRepository;

    public String createVnPayPaymentUrl(Object booking, String ipAddress) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = "";
        String vnp_OrderInfo = "";
        BigDecimal depositAmount = BigDecimal.ZERO;

        String vnp_ReturnUrl = "";

        if (booking instanceof RestaurantBooking rb) {
            vnp_TxnRef = rb.getVnpTxnRef();
            vnp_OrderInfo = "Thanh toan dat ban #" + rb.getBookingId();
            depositAmount = rb.getDepositAmount();

            vnp_ReturnUrl = vnPayConfig.getReturnUrl();
        } else if (booking instanceof HomestayBooking hb) {
            vnp_TxnRef = hb.getVnpTxnRef();
            vnp_OrderInfo = "Thanh toan dat phong #" + hb.getBookingId();
            depositAmount = hb.getDepositAmount();

            vnp_ReturnUrl = vnPayConfig.getReturnUrlHomestay();
        } else {
            throw new IllegalArgumentException("Loại đơn đặt hàng không hợp lệ");
        }
        String vnp_IpAddr = (ipAddress != null) ? ipAddress : "127.0.0.1";
        String vnp_TmnCode = vnPayConfig.getTmnCode();

        BigDecimal amountInVND = depositAmount.setScale(0, RoundingMode.HALF_UP);
        long amount = amountInVND.multiply(new BigDecimal(100)).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        LocalDateTime now = LocalDateTime.now();
        String vnp_CreateDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        String vnp_ExpireDate = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding Error");
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayHashUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getPayUrl() + "?" + queryUrl;
    }

    @Transactional
    public Object processVnPayCallback(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        Map<String, String> cleanParams = new HashMap<>(params);
        cleanParams.remove("vnp_SecureHashType");
        cleanParams.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(cleanParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = cleanParams.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext())
                        hashData.append('&');
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding Error");
        }

        String calculatedHash = VNPayHashUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        if (!calculatedHash.equals(vnp_SecureHash)) {
            throw new RuntimeException("Invalid Signature");
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpPayDateStr = params.get("vnp_PayDate");
        LocalDateTime payTime = parsePayDate(vnpPayDateStr);

        Optional<RestaurantBooking> resOpt = bookingRepository.findByVnpTxnRef(vnpTxnRef);
        if (resOpt.isPresent()) {
            RestaurantBooking booking = resOpt.get();
            updateBookingStatus(booking, vnpResponseCode, payTime);
            return bookingRepository.save(booking);
        }

        Optional<HomestayBooking> homeOpt = homestayBookingRepository.findByVnpTxnRef(vnpTxnRef);
        if (homeOpt.isPresent()) {
            HomestayBooking booking = homeOpt.get();
            updateBookingStatus(booking, vnpResponseCode, payTime);
            return homestayBookingRepository.save(booking);
        }

        throw new RuntimeException("Không tìm thấy đơn hàng với mã giao dịch: " + vnpTxnRef);
    }

    private void updateBookingStatus(Object booking, String responseCode, LocalDateTime payTime) {
        BookingStatus status = "00".equals(responseCode) ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED;

        if (booking instanceof RestaurantBooking rb) {
            rb.setStatus(status);
            if (status == BookingStatus.CONFIRMED)
                rb.setPaymentTime(payTime);
        } else if (booking instanceof HomestayBooking hb) {
            hb.setStatus(status);
            if (status == BookingStatus.CONFIRMED)
                hb.setPaymentTime(payTime);
        }
    }

    private LocalDateTime parsePayDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return LocalDateTime.now();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public VNPayQueryResponse queryTransaction(String vnpTxnRef, String txnDate) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "querydr";
        String vnp_TmnCode = vnPayConfig.getTmnCode();
        String vnp_OrderInfo = "Truy van giao dich " + vnpTxnRef;
        String vnp_IpAddr = "127.0.0.1";
        String vnp_RequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String vnp_CreateDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_TransactionDate", txnDate);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_RequestId", vnp_RequestId);

        String dataToHash = String.join("|",
                vnp_RequestId, vnp_Version, vnp_Command, vnp_TmnCode,
                vnpTxnRef, txnDate, vnp_CreateDate, vnp_IpAddr, vnp_OrderInfo);

        String secureHash = VNPayHashUtil.hmacSHA512(vnPayConfig.getHashSecret(), dataToHash);
        vnp_Params.put("vnp_SecureHashType", "HmacSHA512");
        vnp_Params.put("vnp_SecureHash", secureHash);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(vnp_Params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    vnPayConfig.getQueryUrl(),
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, String>>() {
                    });

            Map<String, String> body = response.getBody();
            if (body == null)
                throw new RuntimeException("VNPay response body is null");

            VNPayQueryResponse result = new VNPayQueryResponse();
            result.setResponseCode(body.get("vnp_ResponseCode"));
            result.setTransactionStatus(body.get("vnp_TransactionStatus"));
            result.setMessage(body.get("vnp_Message"));
            result.setPayDate(body.get("vnp_PayDate"));

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi truy vấn giao dịch từ VNPay", e);
        }
    }

    public VNPayRefundResponse refundTransaction(String vnpTxnRef, BigDecimal amount, String transactionDate,
            String createdBy) {

        String vnp_RequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        String vnp_Version = "2.1.0";
        String vnp_Command = "refund";
        String vnp_TmnCode = vnPayConfig.getTmnCode();
        String vnp_TransactionType = "02";
        BigDecimal amountInVND = amount.setScale(0, RoundingMode.HALF_UP);
        String vnp_Amount = String.valueOf(amountInVND.multiply(new BigDecimal(100)).longValue());

        String vnp_CreateBy = createdBy;
        String vnp_CreateDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String vnp_IpAddr = "127.0.0.1";
        String vnp_OrderInfo = "Hoan tien giao dich " + vnpTxnRef;
        String vnp_TransactionNo = "";

        Map<String, Object> params = new HashMap<>();
        params.put("vnp_RequestId", vnp_RequestId);
        params.put("vnp_Version", vnp_Version);
        params.put("vnp_Command", vnp_Command);
        params.put("vnp_TmnCode", vnp_TmnCode);
        params.put("vnp_TransactionType", vnp_TransactionType);
        params.put("vnp_TxnRef", vnpTxnRef);
        params.put("vnp_Amount", Long.parseLong(vnp_Amount));
        params.put("vnp_OrderInfo", vnp_OrderInfo);
        params.put("vnp_TransactionNo", vnp_TransactionNo);
        params.put("vnp_TransactionDate", Long.parseLong(transactionDate));
        params.put("vnp_CreateBy", vnp_CreateBy);
        params.put("vnp_CreateDate", Long.parseLong(vnp_CreateDate));
        params.put("vnp_IpAddr", vnp_IpAddr);

        String hashData = String.join("|",
                vnp_RequestId, vnp_Version, vnp_Command, vnp_TmnCode,
                vnp_TransactionType, vnpTxnRef, vnp_Amount, vnp_TransactionNo, transactionDate,
                vnp_CreateBy,
                vnp_CreateDate,
                vnp_IpAddr,
                vnp_OrderInfo);

        String vnp_SecureHash = VNPayHashUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        params.put("vnp_SecureHash", vnp_SecureHash);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<VNPayRefundResponse> response = restTemplate.exchange(
                    vnPayConfig.getQueryUrl(),
                    HttpMethod.POST,
                    entity,
                    VNPayRefundResponse.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gọi API hoàn tiền VNPay: " + e.getMessage());
        }
    }
}
