package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.report.ReportResponse;
import com.luxestay.hotel.service.ReportService;
import com.luxestay.hotel.util.AuthorizationHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
public class AdminReportController {
    private final ReportService reportService;
    private final AuthorizationHelper authHelper;

    @GetMapping("/overview")
    public ResponseEntity<ReportResponse> overview(
            HttpServletRequest httpRequest,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        authHelper.requireAdmin(httpRequest);
        return ResponseEntity.ok(reportService.getOverview(from, to, groupBy));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            HttpServletRequest httpRequest,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "groupBy", required = false, defaultValue = "day") String groupBy
    ) {
        authHelper.requireAdmin(httpRequest);
        var report = reportService.getOverview(from, to, groupBy);

        StringBuilder sb = new StringBuilder();
        sb.append("Date,Revenue,Bookings,Cancellations,Occupancy(%)\n");
        for (var p : report.getSeries()) {
            sb.append(p.getDate()).append(',')
              .append(p.getRevenue() == null ? 0 : p.getRevenue()).append(',')
              .append(p.getBookings() == null ? 0 : p.getBookings()).append(',')
              .append(p.getCancellations() == null ? 0 : p.getCancellations()).append(',')
              .append(p.getOccupancy() == null ? 0 : String.format(java.util.Locale.US, "%.2f", p.getOccupancy()))
              .append('\n');
        }

        String filename = String.format("report_%s_%s_%s.csv", groupBy, from, to);
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}


