package com.luxestay.hotel.controller;

import com.luxestay.hotel.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
public class ChatController {
    private final GeminiChatService geminiChatService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message is required"));
        }

        String response = geminiChatService.chat(message);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
