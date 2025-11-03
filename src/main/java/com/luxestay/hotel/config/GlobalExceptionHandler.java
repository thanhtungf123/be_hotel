package com.luxestay.hotel.config;

import com.luxestay.hotel.exception.OverlapException;  
import org.springframework.http.HttpStatus;          
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // === Lỗi trùng lịch: trả 409 + dữ liệu để FE hiện popup/khóa ngày
    @ExceptionHandler(OverlapException.class)
    public ResponseEntity<?> handleOverlap(OverlapException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", ex.getMessage(),
                "availableFrom", ex.getAvailableFrom(),
                "blocked", ex.getBlocked()    // List<Map<String, String>> [{start,end},...]
        ));
    }
}
