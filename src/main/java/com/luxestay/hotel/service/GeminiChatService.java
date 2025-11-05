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
                        String responseText = text.asText();
                        
                        // Tự động thêm link đặt phòng nếu response liên quan đến đặt phòng/xem phòng
                        String lowerResponse = responseText.toLowerCase();
                        boolean isBookingRelated = lowerResponse.contains("đặt phòng") || 
                                                   lowerResponse.contains("xem phòng") ||
                                                   lowerResponse.contains("tìm phòng") ||
                                                   lowerResponse.contains("phòng nào") ||
                                                   lowerResponse.contains("giá phòng") ||
                                                   lowerResponse.contains("booking") ||
                                                   lowerResponse.contains("đặt chỗ") ||
                                                   lowerResponse.contains("reservation");
                        
                        // Nếu chưa có BOOKING_LINK và liên quan đến booking, thêm vào
                        if (isBookingRelated && !responseText.contains("[BOOKING_LINK")) {
                            responseText += "\n\n[BOOKING_LINK:/search]";
                        }
                        
                        return responseText;
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
        // System prompt để chatbot chỉ trả lời về khách sạn và dự án
        String systemPrompt = """
            Bạn là trợ lý AI chuyên về khách sạn Aurora Palace. 
            
            Vai trò của bạn:
            - Chỉ trả lời các câu hỏi liên quan đến khách sạn Aurora Palace và hệ thống quản lý khách sạn
            - Các chủ đề bạn có thể trả lời:
              + Thông tin về phòng nghỉ (loại phòng, giá cả, tiện nghi, sức chứa)
              + Dịch vụ khách sạn (đặt phòng, check-in, check-out, hủy đặt phòng)
              + Thanh toán và chính sách (cọc, thanh toán đủ, hoàn tiền)
              + Tiện nghi và tiện ích (WiFi, bãi đỗ xe, nhà hàng, spa, gym)
              + Đánh giá và phản hồi từ khách hàng
              + Hướng dẫn sử dụng hệ thống đặt phòng online
              + Câu hỏi về tài khoản, đăng nhập, đăng ký
              + Báo cáo và thống kê (dành cho admin)
              + Lịch sử đặt phòng và quản lý booking
            
            Quy tắc:
            - Nếu câu hỏi KHÔNG liên quan đến khách sạn hoặc hệ thống quản lý khách sạn, hãy lịch sự từ chối và hướng dẫn người dùng hỏi về khách sạn
            - Luôn trả lời bằng tiếng Việt một cách thân thiện, chuyên nghiệp
            - Nếu không chắc chắn về thông tin, hãy đề nghị người dùng liên hệ trực tiếp với khách sạn
            - Luôn tập trung vào việc hỗ trợ khách hàng và giải đáp thắc mắc về khách sạn
            
            QUAN TRỌNG - Hiển thị link đặt phòng:
            - Khi người dùng hỏi về đặt phòng, xem phòng, hoặc có ý định đặt phòng, hãy kết thúc câu trả lời bằng dòng:
              "[BOOKING_LINK:/search]"
            - Điều này sẽ hiển thị nút "Đặt phòng ngay" để người dùng có thể click vào
            - Ví dụ: "Chúng tôi có nhiều phòng đẹp với giá cả hợp lý. Bạn có muốn xem và đặt phòng không? [BOOKING_LINK:/search]"
            
            Ví dụ cách từ chối câu hỏi không liên quan:
            "Xin lỗi, tôi là trợ lý chuyên về khách sạn Aurora Palace. Tôi chỉ có thể trả lời các câu hỏi về đặt phòng, dịch vụ khách sạn, tiện nghi, và các vấn đề liên quan đến hệ thống quản lý khách sạn. Bạn có câu hỏi nào về khách sạn không?"
            """;
        
        return systemPrompt + "\n\nNgười dùng hỏi: " + userMessage + "\n\nHãy trả lời một cách tự nhiên và hữu ích:";
    }
}

