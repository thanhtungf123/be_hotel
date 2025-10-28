package com.luxestay.hotel.exception;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class OverlapException extends RuntimeException {
    private final LocalDate availableFrom;
    private final List<Map<String, String>> blocked;

    public OverlapException(String message, LocalDate availableFrom,
                            List<Map<String, String>> blocked) {
        super(message);
        this.availableFrom = availableFrom;
        this.blocked = blocked;
    }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public List<Map<String, String>> getBlocked() { return blocked; }
}
