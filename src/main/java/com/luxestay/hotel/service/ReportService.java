package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.report.ReportResponse;

import java.time.LocalDate;

public interface ReportService {
    ReportResponse getOverview(LocalDate from, LocalDate to, String groupBy);
}
