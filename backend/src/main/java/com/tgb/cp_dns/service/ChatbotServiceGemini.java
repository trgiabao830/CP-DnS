package com.tgb.cp_dns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgb.cp_dns.dto.homestay.HomestaySearchRequest;
import com.tgb.cp_dns.dto.restaurant.BookingSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceGemini {

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Value("${gemini.api.url}")
    private String geminiUrlBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private final ClientBookingService restaurantService;
    private final ClientBookingHomestayService homestayService;

    private final Map<String, List<Map<String, Object>>> chatHistory = new ConcurrentHashMap<>();

    private static final String SYSTEM_INSTRUCTION_TEXT = "Bạn là trợ lý AI thông minh của hệ thống Nhà hàng & Homestay (CP-DNS). "
            +
            "Nhiệm vụ: Hỗ trợ tìm kiếm, kiểm tra tính hợp lệ, thu thập thông tin cá nhân và ĐIỀU HƯỚNG khách hàng. " +

            "THỜI GIAN HỆ THỐNG HIỆN TẠI: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " (Năm-Tháng-Ngày Giờ:Phút). "
            +

            "QUY TẮC KIỂM TRA THỜI GIAN (ƯU TIÊN SỐ 1): " +
            "1. Trước khi tìm kiếm, PHẢI so sánh thời gian khách chọn với thời gian hiện tại. " +
            "2. Nguyên tắc: Không được đặt trong quá khứ. " +
            "   - Ví dụ: Hiện tại 22:00 ngày 17/02, khách đặt 18:00 ngày 17/02 -> TỪ CHỐI. " +
            "   - Nhà hàng: Giờ đặt phải trễ hơn hiện tại ít nhất 30 phút. " +
            "   - Homestay: Ngày Check-in phải từ hôm nay trở đi. " +
            "3. Khi từ chối: 'Xin lỗi, thời gian bạn chọn không hợp lệ (đã qua). Hiện tại là [Giờ hiện tại], vui lòng chọn lại.'. "
            +

            "QUY TẮC THU THẬP THÔNG TIN (BẮT BUỘC): " +
            "1. Sau khi khách chốt Bàn hoặc Phòng, bạn PHẢI yêu cầu khách cung cấp đủ 3 thông tin: **Họ tên**, **Số điện thoại** và **Email**. "
            +
            "2. Đây là thông tin BẮT BUỘC để gửi vé điện tử. Nếu khách cung cấp thiếu (ví dụ quên Email), hãy hỏi lại cho đến khi đủ. "
            +
            "3. Câu mẫu: 'Để giữ chỗ và nhận xác nhận qua Email, bạn vui lòng cung cấp: Họ tên, Số điện thoại và Email nhé?'. "
            +

            "QUY TẮC NGHIỆP VỤ NHÀ HÀNG: " +
            "1. Bước 1: Kiểm tra giờ đặt hợp lệ -> Tool `search_available_tables`. " +
            "2. Bước 2: Khách chọn bàn -> Hỏi Họ tên, SĐT, Email (nếu chưa có). " +
            "3. Bước 3: Hỏi 'Bạn có muốn đặt món trước không?'. " +
            "4. Bước 4 - Tạo Link Điều Hướng (đủ tham số): " +
            "   - CÓ ĐẶT MÓN: `[Nhấn để chọn món](/restaurant/booking/menu?date={...}&time={...}&guests={...}&tableId={...}&name={...}&phone={...}&email={...})` "
            +
            "   - KHÔNG ĐẶT MÓN: `[Nhấn để hoàn tất](/restaurant/booking/info?date={...}&time={...}&guests={...}&tableId={...}&name={...}&phone={...}&email={...})` "
            +

            "QUY TẮC NGHIỆP VỤ HOMESTAY: " +
            "1. Bước 1: Kiểm tra ngày check-in hợp lệ -> Tool `search_homestay_rooms`. " +
            "2. Bước 2: Khách chọn phòng -> Hỏi Họ tên, SĐT, Email (nếu chưa có). " +
            "3. Bước 3 - Tạo Link Điều Hướng: " +
            "   - Link: `[Nhấn để xác nhận đặt phòng](/homestay/room/{id}?checkIn={...}&checkOut={...}&adults={...}&name={...}&phone={...}&email={...})` "
            +

            "QUY TẮC TRẢ LỜI: " +
            "1. Tuyệt đối KHÔNG báo 'Đặt thành công' khi chưa có link. " +
            "2. Link dùng Markdown. Thay khoảng trắng trong Tên bằng `%20` hoặc `+`.";

    @SuppressWarnings("unchecked")
    public String chatWithGemini(String userMessage, String sessionId) {
        String url = geminiUrlBase.trim() + geminiKey.trim();

        if (sessionId == null || sessionId.isEmpty())
            sessionId = "guest-session";

        try {
            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> systemPart = Map.of("text", SYSTEM_INSTRUCTION_TEXT);
            requestBody.put("system_instruction", Map.of("parts", List.of(systemPart)));

            requestBody.put("tools", List.of(Map.of("function_declarations", getGeminiToolsDefinition())));

            List<Map<String, Object>> currentConversation = chatHistory.getOrDefault(sessionId, new ArrayList<>());

            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", List.of(Map.of("text", userMessage)));

            currentConversation.add(userContent);
            requestBody.put("contents", currentConversation);

            Map<String, Object> responseBody = callGeminiApi(url, requestBody);
            if (responseBody == null)
                return "Hệ thống đang bận, vui lòng thử lại.";

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty())
                return "Không nhận được phản hồi.";

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            Map<String, Object> firstPart = parts.get(0);

            if (firstPart.containsKey("functionCall")) {
                Map<String, Object> functionCall = (Map<String, Object>) firstPart.get("functionCall");
                String functionName = (String) functionCall.get("name");
                Map<String, Object> args = (Map<String, Object>) functionCall.get("args");

                log.info("Gemini gọi hàm: {} | Args: {}", functionName, args);

                String functionResultJson = executeLocalFunction(functionName, args);

                Map<String, Object> modelResponse = new HashMap<>();
                modelResponse.put("role", "model");
                modelResponse.put("parts", parts);
                currentConversation.add(modelResponse);

                Map<String, Object> functionResponseData = new HashMap<>();
                functionResponseData.put("name", functionName);
                functionResponseData.put("response", Map.of("content", functionResultJson));

                Map<String, Object> functionResponseContent = new HashMap<>();
                functionResponseContent.put("role", "function");
                functionResponseContent.put("parts", List.of(Map.of("functionResponse", functionResponseData)));
                currentConversation.add(functionResponseContent);

                requestBody.put("contents", currentConversation);
                Map<String, Object> finalResponse = callGeminiApi(url, requestBody);

                String finalText = extractTextFromResponse(finalResponse);

                saveBotResponseToHistory(sessionId, currentConversation, finalText);

                return finalText;
            }

            String aiText = (String) firstPart.get("text");
            saveBotResponseToHistory(sessionId, currentConversation, aiText);

            return aiText;

        } catch (Exception e) {
            log.error("Lỗi Chatbot: ", e);
            return "Xin lỗi, hệ thống đang gặp sự cố kỹ thuật.";
        }
    }

    private void saveBotResponseToHistory(String sessionId, List<Map<String, Object>> conversation, String text) {
        Map<String, Object> modelContent = new HashMap<>();
        modelContent.put("role", "model");
        modelContent.put("parts", List.of(Map.of("text", text)));
        conversation.add(modelContent);
        chatHistory.put(sessionId, conversation);
    }

    private Map<String, Object> callGeminiApi(String url, Map<String, Object> body) {
        int maxRetries = 3;
        int retryCount = 0;
        long waitTime = 2000;

        while (retryCount < maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                        });
                return response.getBody();

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ||
                        e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {

                    retryCount++;
                    log.warn("Gemini quá tải (Lỗi {}). Thử lại lần {}/{}...", e.getStatusCode(), retryCount,
                            maxRetries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    waitTime *= 2;
                } else {
                    log.error("Lỗi API không thể thử lại: {}", e.getResponseBodyAsString());
                    throw e;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        if (responseBody == null)
            return "";
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        if (candidates == null || candidates.isEmpty())
            return "";

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty())
            return "";

        return (String) parts.get(0).get("text");
    }

    private List<Map<String, Object>> getGeminiToolsDefinition() {
        return List.of(
                Map.of("name", "search_available_tables", "description", "Tìm bàn trống.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of(
                                "date", Map.of("type", "STRING", "description", "yyyy-MM-dd"),
                                "time", Map.of("type", "STRING", "description", "HH:mm"),
                                "guests", Map.of("type", "INTEGER", "description", "Số khách")), "required",
                                List.of("date", "time", "guests"))),
                Map.of("name", "search_homestay_rooms", "description", "Tìm phòng Homestay trống.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of(
                                "checkIn", Map.of("type", "STRING", "description", "yyyy-MM-dd"),
                                "checkOut", Map.of("type", "STRING", "description", "yyyy-MM-dd"),
                                "adults", Map.of("type", "INTEGER", "description", "Số người lớn"),
                                "children", Map.of("type", "INTEGER", "description", "Số trẻ em")), "required",
                                List.of("checkIn", "checkOut", "adults"))),
                Map.of("name", "get_menu", "description", "Xem danh sách món ăn.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of(
                                "keyword", Map.of("type", "STRING", "description", "Tên món")))),
                Map.of("name", "check_booking_detail", "description", "Tra cứu đơn hàng.",
                        "parameters", Map.of("type", "OBJECT", "properties", Map.of(
                                "accessToken", Map.of("type", "STRING", "description", "Mã đơn")), "required",
                                List.of("accessToken"))));
    }

    private String executeLocalFunction(String functionName, Map<String, Object> args) {
        try {
            switch (functionName) {
                case "search_available_tables":
                    BookingSearchRequest tableReq = new BookingSearchRequest();
                    tableReq.setDate(LocalDate.parse((String) args.get("date")));
                    tableReq.setTime(LocalTime.parse((String) args.get("time")));
                    tableReq.setNumberOfGuests(((Number) args.get("guests")).intValue());
                    var tables = restaurantService.searchAvailableTables(tableReq);
                    return tables.isEmpty() ? "Hết bàn." : objectMapper.writeValueAsString(tables);

                case "search_homestay_rooms":
                    HomestaySearchRequest roomReq = new HomestaySearchRequest();
                    roomReq.setCheckInDate(LocalDate.parse((String) args.get("checkIn")));
                    roomReq.setCheckOutDate(LocalDate.parse((String) args.get("checkOut")));
                    roomReq.setNumberOfAdults(((Number) args.get("adults")).intValue());
                    Object children = args.get("children");
                    roomReq.setNumberOfChildren(children != null ? ((Number) children).intValue() : 0);
                    var rooms = homestayService.searchAvailableRoomTypes(roomReq);
                    return rooms.isEmpty() ? "Hết phòng." : objectMapper.writeValueAsString(rooms);

                case "get_menu":
                    String keyword = (String) args.getOrDefault("keyword", "");
                    var menu = restaurantService.getMenuForBooking(keyword, LocalDate.now());
                    if (menu.size() > 10)
                        menu = menu.subList(0, 10);
                    return objectMapper.writeValueAsString(menu);

                case "check_booking_detail":
                    String token = (String) args.get("accessToken");
                    try {
                        return "Nhà hàng: "
                                + objectMapper.writeValueAsString(restaurantService.getBookingDetail(token));
                    } catch (Exception e) {
                        try {
                            return "Homestay: "
                                    + objectMapper.writeValueAsString(homestayService.getHomestayBookingDetail(token));
                        } catch (Exception ex) {
                            return "Không tìm thấy đơn hàng.";
                        }
                    }
                default:
                    return "Hàm không hỗ trợ.";
            }
        } catch (Exception e) {
            log.error("Lỗi local function: ", e);
            return "Lỗi dữ liệu: " + e.getMessage();
        }
    }
}