package com.luxestay.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:AIzaSyCXQ00SlM_SjrHCbJ7MQxFYQmOGG78UWUA}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = 
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public GeminiChatService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String userMessage) {
        try {
            // Debug: Log API key và URL
            System.out.println("=== Gemini Chat Debug ===");
            System.out.println("API Key (first 10 chars): " + (geminiApiKey != null ? geminiApiKey.substring(0, Math.min(10, geminiApiKey.length())) : "NULL"));
            System.out.println("API URL: " + GEMINI_API_URL);
            
            // Create request body with proper structure
            Map<String, Object> requestBody = new HashMap<>();
            
            // Create parts list
            List<Map<String, Object>> partsList = new ArrayList<>();
            Map<String, Object> partMap = new HashMap<>();
            partMap.put("text", buildPrompt(userMessage));
            partsList.add(partMap);
            
            // Create contents list
            List<Map<String, Object>> contentsList = new ArrayList<>();
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("parts", partsList);
            contentsList.add(contentMap);
            
            requestBody.put("contents", contentsList);

            // Set headers - use X-goog-api-key in header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
                headers.set("X-goog-api-key", geminiApiKey);
                System.out.println("Header X-goog-api-key set successfully");
            } else {
                System.err.println("WARNING: geminiApiKey is null or empty!");
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call API - key should be in header, not query param
            System.out.println("Calling Gemini API...");
            ResponseEntity<String> response = restTemplate.exchange(
                    GEMINI_API_URL, HttpMethod.POST, request, String.class);

            // Parse response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody();
                System.out.println("Gemini API Response: " + responseBody); // Debug log
                
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // Check for errors first
                if (jsonNode.has("error")) {
                    JsonNode error = jsonNode.get("error");
                    String errorMsg = error.has("message") ? error.get("message").asText() : "Unknown error";
                    System.err.println("Gemini API Error: " + errorMsg);
                    return "Xin lỗi, có lỗi xảy ra: " + errorMsg;
                }
                
                JsonNode candidates = jsonNode.get("candidates");
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode content = firstCandidate.get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            JsonNode firstPart = parts.get(0);
                            JsonNode text = firstPart.get("text");
                            if (text != null) {
                                return text.asText();
                            }
                        }
                    }
                }
            }

            return "Xin lỗi, tôi không thể trả lời câu hỏi này. Vui lòng thử lại.";

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return "Lỗi kết nối API: " + e.getStatusCode() + ". Vui lòng kiểm tra API key.";
        } catch (Exception e) {
            System.err.println("Error in GeminiChatService: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi khi kết nối với AI: " + e.getMessage();
        }
    }

    private String buildPrompt(String userMessage) {
        // System prompt để chatbot hoạt động như AI thông thường, đồng thời hỗ trợ khách sạn
        String systemPrompt = """
            Bạn là một trợ lý AI thông minh và thân thiện của khách sạn Aurora Palace. 
            
            Về vai trò của bạn:
            - Trước tiên, bạn là trợ lý chuyên về khách sạn: có thể trả lời các câu hỏi về phòng nghỉ, dịch vụ, đặt phòng, thanh toán, tiện nghi, giá cả, chính sách hủy.
            - Đồng thời, bạn cũng là một AI thông thường: có thể trả lời các câu hỏi về nhiều chủ đề khác nhau như kiến thức tổng quát, giải thích khái niệm, đưa ra lời khuyên, giải đáp thắc mắc, v.v.
            
            Phong cách giao tiếp:
            - Luôn trả lời một cách thân thiện, chuyên nghiệp, và hữu ích
            - Trả lời bằng tiếng Việt một cách tự nhiên và dễ hiểu
            - Nếu câu hỏi về khách sạn, hãy tập trung vào thông tin khách sạn
            - Nếu câu hỏi về chủ đề khác, hãy trả lời như một AI thông thường, không bắt buộc phải liên hệ với khách sạn
            - Luôn sẵn sàng giúp đỡ và tạo trải nghiệm tích cực cho người dùng
            """;
        
        return systemPrompt + "\n\nNgười dùng hỏi: " + userMessage + "\n\nHãy trả lời một cách tự nhiên và hữu ích:";
    }
}

