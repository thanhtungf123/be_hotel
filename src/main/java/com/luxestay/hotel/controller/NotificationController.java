package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.NotificationEntity;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final AuthService authService;
    
    /**
     * Lấy tất cả notifications của user hiện tại (có phân trang)
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            if (token == null || token.isEmpty()) {
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("items", List.of());
                emptyResponse.put("page", 0);
                emptyResponse.put("size", size);
                emptyResponse.put("total", 0);
                emptyResponse.put("totalPages", 0);
                return ResponseEntity.ok(emptyResponse);
            }
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationEntity> notifications = notificationService.getUserNotifications(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", notifications.getContent());
            response.put("page", notifications.getNumber());
            response.put("size", notifications.getSize());
            response.put("total", notifications.getTotalElements());
            response.put("totalPages", notifications.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Token không hợp lệ - return empty list
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("items", List.of());
            emptyResponse.put("page", 0);
            emptyResponse.put("size", size);
            emptyResponse.put("total", 0);
            emptyResponse.put("totalPages", 0);
            return ResponseEntity.ok(emptyResponse);
        } catch (Exception e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Lấy notifications chưa đọc
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            List<NotificationEntity> unread = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(unread);
        } catch (IllegalArgumentException e) {
            // Token không hợp lệ - return empty list
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            System.err.println("Error fetching unread notifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }
    
    /**
     * Đếm số notifications chưa đọc
     * GET /api/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                System.out.println("[NotificationController] No token provided for /unread/count");
                return ResponseEntity.ok(Map.of("count", 0));
            }
            System.out.println("[NotificationController] Token received: " + token.substring(0, Math.min(20, token.length())) + "...");
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            System.out.println("[NotificationController] User ID: " + userId);
            Long count = notificationService.countUnread(userId);
            System.out.println("[NotificationController] Unread count: " + count);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalArgumentException e) {
            // Token không hợp lệ hoặc user chưa login - return 0 count
            System.out.println("[NotificationController] Invalid token: " + e.getMessage());
            return ResponseEntity.ok(Map.of("count", 0));
        } catch (Exception e) {
            // Log error for debugging
            System.err.println("[NotificationController] Error fetching notification count: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }
    
    /**
     * Đánh dấu một notification là đã đọc
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            notificationService.markAsRead(id, userId);
            return ResponseEntity.ok(Map.of("message", "Marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Đánh dấu tất cả notifications là đã đọc
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Xóa một notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authentication required"));
            }
            Account account = authService.requireAccount(token);
            Integer userId = account.getId();
            notificationService.deleteNotification(id, userId);
            return ResponseEntity.ok(Map.of("message", "Notification deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

