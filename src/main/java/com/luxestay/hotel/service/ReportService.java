package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.report.ReportResponse;
import com.luxestay.hotel.dto.report.ReportSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final EntityManager em;
    private final com.luxestay.hotel.repository.RoomRepository roomRepository;

    private static class CacheEntry {
        final ReportResponse data;
        final long expireAt;
        CacheEntry(ReportResponse d, long t) { this.data = d; this.expireAt = t; }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 60_000; // cache 60s

    public ReportResponse getOverview(LocalDate from, LocalDate to, String groupBy) {
        if (from == null || to == null)
            throw new IllegalArgumentException("from/to is required (YYYY-MM-DD)");
        if (to.isBefore(from))
            throw new IllegalArgumentException("to must be >= from");

        String key = (groupBy == null ? "day" : groupBy.toLowerCase()) + "|" + from + "|" + to;
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expireAt > System.currentTimeMillis()) {
            return cached.data;
        }

        String bucketExpr = switch (groupBy == null ? "day" : groupBy.toLowerCase()) {
            case "week" -> "DATEADD(week, DATEDIFF(week, 0, b.check_in_date), 0)";
            case "month" -> "DATEFROMPARTS(YEAR(b.check_in_date), MONTH(b.check_in_date), 1)";
            default -> "CAST(b.check_in_date AS date)";
        };

        String sql = """
            SELECT %s AS bucket,
                   SUM(CASE WHEN b.payment_state IN ('deposit_paid','paid_in_full')
                             AND LOWER(b.status) IN ('confirmed','checked_in','completed')
                        THEN ISNULL(b.total_price,0) ELSE 0 END) AS revenue,
                   COUNT(*) AS bookings,
                   SUM(CASE WHEN LOWER(b.status)='cancelled' THEN 1 ELSE 0 END) AS cancellations
            FROM bookings b
            WHERE b.check_in_date >= :from AND b.check_in_date <= :to
            GROUP BY %s
            ORDER BY bucket
        """.formatted(bucketExpr, bucketExpr);

        Query q = em.createNativeQuery(sql);
        q.setParameter("from", from);
        q.setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<com.luxestay.hotel.dto.report.ReportSeriesPoint> series = new ArrayList<>();
        int rooms = (int) roomRepository.count();
        if (rooms <= 0) rooms = 1;

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalBookings = 0;
        int totalCancels = 0;

        for (Object[] r : rows) {
            LocalDate bucket = (r[0] instanceof java.sql.Date d) ? d.toLocalDate() : (LocalDate) r[0];
            BigDecimal revenue = toBigDecimal(r[1]);
            int bookings = toInt(r[2]);
            int cancellations = toInt(r[3]);

            totalRevenue = totalRevenue.add(revenue);
            totalBookings += bookings;
            totalCancels += cancellations;

            double occupancy = rooms == 0 ? 0d : (bookings * 100.0) / rooms;

            series.add(com.luxestay.hotel.dto.report.ReportSeriesPoint.builder()
                    .date(bucket)
                    .revenue(revenue)
                    .bookings(bookings)
                    .cancellations(cancellations)
                    .occupancy(occupancy)
                    .build());
        }

        double cancelRate = totalBookings == 0 ? 0d : (totalCancels * 100.0) / totalBookings;
        BigDecimal avgRevPerBooking = totalBookings == 0 ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(totalBookings), java.math.RoundingMode.HALF_UP);

        double avgOcc = 0d;
        if (!series.isEmpty()) {
            double sum = 0d;
            for (var p : series)
                sum += (p.getOccupancy() == null ? 0d : p.getOccupancy());
            avgOcc = sum / series.size();
        }

        ReportSummary summary = ReportSummary.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .cancelledBookings(totalCancels)
                .cancellationRate(cancelRate)
                .occupancyRate(avgOcc)
                .avgRevenuePerBooking(avgRevPerBooking)
                .build();

        ReportResponse resp = ReportResponse.builder()
                .summary(summary)
                .series(series)
                .build();

        cache.put(key, new CacheEntry(resp, System.currentTimeMillis() + TTL_MS));
        return resp;
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }
}
