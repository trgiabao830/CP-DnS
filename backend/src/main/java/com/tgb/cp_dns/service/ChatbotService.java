package com.tgb.cp_dns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgb.cp_dns.config.PromptLoader;
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
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    @Value("${groq.api.key}")
    private String groqKey;

    @Value("${groq.api.url}")
    private String groqUrl;

    @Value("${groq.model}")
    private String groqModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private final ClientBookingService restaurantService;
    private final ClientBookingHomestayService homestayService;
    private final SettingsService settingsService;

    private final Map<String, List<Map<String, Object>>> chatHistory = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public String chatWithGroq(String userMessage, String sessionId) {
        if (sessionId == null || sessionId.isEmpty())
            sessionId = "guest-session";

        try {
            List<Map<String, Object>> messages = chatHistory.getOrDefault(sessionId, new ArrayList<>());

            if (messages.isEmpty()) {
                String openTime = settingsService.getRestaurantOpeningTime().toString();
                String closeTime = settingsService.getRestaurantClosingTime().toString();

                String systemInstruction = promptLoader.getSystemInstruction()
                        .replace("{OPENING_TIME}", openTime)
                        .replace("{CLOSING_TIME}", closeTime);

                Map<String, Object> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemInstruction);
                messages.add(systemMsg);
            }

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            Map<String, Object> responseBody = callGroqApi(messages);
            if (responseBody == null)
                return "Hệ thống đang bận, vui lòng thử lại.";

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty())
                return "Không nhận được phản hồi.";

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");

            if (message.containsKey("tool_calls")) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");

                messages.add(message);

                for (Map<String, Object> toolCall : toolCalls) {
                    String toolCallId = (String) toolCall.get("id");
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    String functionName = (String) function.get("name");
                    String arguments = (String) function.get("arguments");

                    log.info("Groq gọi hàm: {} | Args: {}", functionName, arguments);

                    Map<String, Object> args = objectMapper.readValue(arguments, Map.class);

                    String functionResult = executeLocalFunction(functionName, args);

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCallId);
                    toolMsg.put("name", functionName);
                    toolMsg.put("content", functionResult);

                    messages.add(toolMsg);
                }

                Map<String, Object> finalResponse = callGroqApi(messages);
                String finalText = extractTextFromGroqResponse(finalResponse);

                addAssistantMessage(sessionId, messages, finalText);
                return finalText;
            }

            String content = (String) message.get("content");
            addAssistantMessage(sessionId, messages, content);
            return content;

        } catch (Exception e) {
            log.error("Lỗi Chatbot Groq: ", e);
            return "Xin lỗi, hệ thống đang gặp sự cố kỹ thuật.";
        }
    }

    private void addAssistantMessage(String sessionId, List<Map<String, Object>> messages, String content) {
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", content);
        messages.add(assistantMsg);
        chatHistory.put(sessionId, messages);
    }

    private Map<String, Object> callGroqApi(List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("messages", messages);
        requestBody.put("tools", getGroqToolsDefinition());
        requestBody.put("tool_choice", "auto");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    groqUrl, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Groq API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGroqResponse(Map<String, Object> responseBody) {
        if (responseBody == null)
            return "";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty())
            return "";
        return (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
    }

    private List<Map<String, Object>> getGroqToolsDefinition() {
        return List.of(
                createTool("search_available_tables", "Tìm bàn trống nhà hàng", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "date", Map.of("type", "string", "description", "yyyy-MM-dd"),
                                "time", Map.of("type", "string", "description", "HH:mm"),
                                "guests", Map.of("type", "integer", "description", "Số khách")),
                        "required", List.of("date", "time", "guests"))),
                createTool("search_homestay_rooms", "Tìm phòng Homestay trống", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "checkIn", Map.of("type", "string", "description", "yyyy-MM-dd"),
                                "checkOut", Map.of("type", "string", "description", "yyyy-MM-dd"),
                                "adults", Map.of("type", "integer", "description", "Số người lớn"),
                                "children", Map.of("type", "integer", "description", "Số trẻ em")),
                        "required", List.of("checkIn", "checkOut", "adults"))),
                createTool("get_menu", "Xem menu món ăn", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "Tên món")))),
                createTool("check_booking_detail", "Tra cứu đơn hàng", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "accessToken", Map.of("type", "string", "description", "Mã đơn")),
                        "required", List.of("accessToken"))));
    }

    private Map<String, Object> createTool(String name, String description, Map<String, Object> parameters) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters));
    }

    private String executeLocalFunction(String functionName, Map<String, Object> args) {
        try {
            switch (functionName) {
                case "search_available_tables":
                    BookingSearchRequest tableReq = new BookingSearchRequest();
                    String dateStr = String.valueOf(args.get("date"));
                    String timeStr = String.valueOf(args.get("time"));
                    LocalTime reqTime = LocalTime.parse(timeStr);
                    LocalTime openT = settingsService.getRestaurantOpeningTime();
                    LocalTime closeT = settingsService.getRestaurantClosingTime();
                    if (reqTime.isBefore(openT) || reqTime.isAfter(closeT)) {
                        return "Thông báo cho khách: Nhà hàng chỉ nhận đặt bàn từ " + openT + " đến " + closeT
                                + ". Vui lòng chọn giờ khác.";
                    }
                    int guests;
                    Object guestsObj = args.get("guests");
                    if (guestsObj instanceof Number) {
                        guests = ((Number) guestsObj).intValue();
                    } else {
                        guests = Integer.parseInt(String.valueOf(guestsObj));
                    }
                    tableReq.setDate(LocalDate.parse(dateStr));
                    tableReq.setTime(reqTime);
                    tableReq.setNumberOfGuests(guests);
                    var tables = restaurantService.searchAvailableTables(tableReq);
                    return tables.isEmpty()
                            ? "Hết bàn vào giờ này."
                            : objectMapper.writeValueAsString(tables);
                case "search_homestay_rooms":
                    HomestaySearchRequest roomReq = new HomestaySearchRequest();
                    roomReq.setCheckInDate(LocalDate.parse((String) args.get("checkIn")));
                    roomReq.setCheckOutDate(LocalDate.parse((String) args.get("checkOut")));
                    roomReq.setNumberOfAdults(((Number) args.get("adults")).intValue());
                    Object children = args.get("children");
                    roomReq.setNumberOfChildren(children != null ? ((Number) children).intValue() : 0);
                    var rooms = homestayService.searchAvailableRoomTypes(roomReq);
                    return rooms.isEmpty() ? "Hết phòng trong ngày này." : objectMapper.writeValueAsString(rooms);

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
                    return "Hàm không được hỗ trợ.";
            }
        } catch (Exception e) {
            log.error("Lỗi chạy hàm local: ", e);
            return "Lỗi dữ liệu: " + e.getMessage();
        }
    }
}