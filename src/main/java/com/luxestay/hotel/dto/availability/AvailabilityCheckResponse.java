package com.luxestay.hotel.dto.availability;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityCheckResponse(
    boolean available,
    LocalDate availableFrom,
    List<DateRange> blocked
) {}
