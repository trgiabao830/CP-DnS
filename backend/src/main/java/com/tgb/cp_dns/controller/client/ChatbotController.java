package com.tgb.cp_dns.controller.client;

import com.tgb.cp_dns.dto.common.ChatMessageRequest;
import com.tgb.cp_dns.dto.common.ChatResponse;
import com.tgb.cp_dns.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // @PostMapping("/chat")
    // public ResponseEntity<ChatResponse> chat(@RequestBody ChatMessageRequest request) {
    //     String sessionId = request.getSessionId();
    //     if (sessionId == null) {
    //         sessionId = "guest-session-" + java.util.UUID.randomUUID().toString();
    //     }

    //     String responseText = chatbotService.chatWithGemini(request.getMessage(), sessionId);
    //     return ResponseEntity.ok(new ChatResponse(responseText));
    // }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatMessageRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = "guest-session-" + java.util.UUID.randomUUID().toString();
        }

        String responseText = chatbotService.chatWithGroq(request.getMessage(), sessionId);
        return ResponseEntity.ok(new ChatResponse(responseText));
    }
}