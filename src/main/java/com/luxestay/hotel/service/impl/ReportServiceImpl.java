package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.report.ReportResponse;
import com.luxestay.hotel.dto.report.ReportSeriesPoint;
import com.luxestay.hotel.dto.report.ReportSummary;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getOverview(LocalDate from, LocalDate to, String groupBy) {
        // Lấy tất cả bookings trong khoảng thời gian
        List<BookingEntity> allBookings = bookingRepository.findAll().stream()
                .filter(b -> {
                    LocalDate checkIn = b.getCheckIn();
                    LocalDate checkOut = b.getCheckOut();
                    if (checkIn == null || checkOut == null) return false;
                    // Booking nằm trong khoảng thời gian nếu checkIn hoặc checkOut nằm trong khoảng
                    return !checkIn.isAfter(to) && !checkOut.isBefore(from);
                })
                .collect(Collectors.toList());

        // Tính summary tổng
        ReportSummary summary = calculateSummary(allBookings);

        // Tính series theo groupBy
        List<ReportSeriesPoint> series = calculateSeries(allBookings, from, to, groupBy);

        return ReportResponse.builder()
                .summary(summary)
                .series(series)
                .build();
    }

    private ReportSummary calculateSummary(List<BookingEntity> bookings) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalBookings = bookings.size();
        int cancelledBookings = 0;
        BigDecimal totalOccupancyDays = BigDecimal.ZERO;
        int totalRoomDays = 0;

        for (BookingEntity b : bookings) {
            // Revenue: chỉ tính booking đã thanh toán (deposit hoặc full)
            if (b.getPaymentState() != null && 
                (b.getPaymentState().equals("deposit_paid") || b.getPaymentState().equals("paid_in_full"))) {
                if (b.getTotalPrice() != null) {
                    totalRevenue = totalRevenue.add(b.getTotalPrice());
                }
            }

            // Cancellations
            if (b.getStatus() != null && b.getStatus().equalsIgnoreCase("cancelled")) {
                cancelledBookings++;
            }

            // Occupancy: tính số ngày đã được đặt
            if (b.getCheckIn() != null && b.getCheckOut() != null && 
                b.getStatus() != null && !b.getStatus().equalsIgnoreCase("cancelled")) {
                long nights = ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
                totalOccupancyDays = totalOccupancyDays.add(BigDecimal.valueOf(nights));
                totalRoomDays += nights;
            }
        }

        // Tính rates
        double cancellationRate = totalBookings > 0 
            ? (double) cancelledBookings / totalBookings * 100.0 
            : 0.0;

        // Tính occupancy rate: cần số phòng tổng và số ngày trong kỳ
        List<RoomEntity> allRooms = roomRepository.findAll();
        int totalRooms = allRooms.size();
        
        // Lấy min/max dates từ bookings hoặc dùng from/to
        LocalDate minDate = bookings.stream()
            .map(BookingEntity::getCheckIn)
            .filter(d -> d != null)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
        LocalDate maxDate = bookings.stream()
            .map(BookingEntity::getCheckOut)
            .filter(d -> d != null)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());
        
        long periodDays = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
        
        double occupancyRate = (totalRooms > 0 && periodDays > 0)
            ? (double) totalRoomDays / (totalRooms * periodDays) * 100.0
            : 0.0;

        BigDecimal avgRevenuePerBooking = totalBookings > 0
            ? totalRevenue.divide(BigDecimal.valueOf(totalBookings), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ReportSummary.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .cancelledBookings(cancelledBookings)
                .cancellationRate(cancellationRate)
                .occupancyRate(occupancyRate)
                .avgRevenuePerBooking(avgRevenuePerBooking)
                .build();
    }

    private List<ReportSeriesPoint> calculateSeries(List<BookingEntity> bookings, LocalDate from, LocalDate to, String groupBy) {
        List<ReportSeriesPoint> series = new ArrayList<>();

        if ("day".equalsIgnoreCase(groupBy)) {
            LocalDate current = from;
            while (!current.isAfter(to)) {
                series.add(calculatePointForDate(bookings, current));
                current = current.plusDays(1);
            }
        } else if ("week".equalsIgnoreCase(groupBy)) {
            LocalDate current = from;
            while (!current.isAfter(to)) {
                LocalDate weekStart = current.with(java.time.DayOfWeek.MONDAY);
                LocalDate weekEnd = weekStart.plusDays(6);
                LocalDate actualEnd = weekEnd.isAfter(to) ? to : weekEnd;
                series.add(calculatePointForPeriod(bookings, weekStart, actualEnd));
                current = weekEnd.plusDays(1);
            }
        } else if ("month".equalsIgnoreCase(groupBy)) {
            LocalDate current = from;
            while (!current.isAfter(to)) {
                LocalDate monthStart = current.withDayOfMonth(1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
                LocalDate actualEnd = monthEnd.isAfter(to) ? to : monthEnd;
                series.add(calculatePointForPeriod(bookings, monthStart, actualEnd));
                current = monthStart.plusMonths(1);
            }
        }

        return series;
    }

    private ReportSeriesPoint calculatePointForDate(List<BookingEntity> bookings, LocalDate date) {
        List<BookingEntity> dayBookings = bookings.stream()
                .filter(b -> {
                    LocalDate checkIn = b.getCheckIn();
                    LocalDate checkOut = b.getCheckOut();
                    return checkIn != null && checkOut != null &&
                           !checkIn.isAfter(date) && !checkOut.isBefore(date);
                })
                .collect(Collectors.toList());

        return calculatePoint(dayBookings, date);
    }

    private ReportSeriesPoint calculatePointForPeriod(List<BookingEntity> bookings, LocalDate start, LocalDate end) {
        List<BookingEntity> periodBookings = bookings.stream()
                .filter(b -> {
                    LocalDate checkIn = b.getCheckIn();
                    LocalDate checkOut = b.getCheckOut();
                    return checkIn != null && checkOut != null &&
                           !checkIn.isAfter(end) && !checkOut.isBefore(start);
                })
                .collect(Collectors.toList());

        return calculatePoint(periodBookings, start);
    }

    private ReportSeriesPoint calculatePoint(List<BookingEntity> bookings, LocalDate date) {
        BigDecimal revenue = BigDecimal.ZERO;
        int bookingCount = bookings.size();
        int cancellations = 0;
        int occupiedNights = 0;

        for (BookingEntity b : bookings) {
            if (b.getPaymentState() != null && 
                (b.getPaymentState().equals("deposit_paid") || b.getPaymentState().equals("paid_in_full"))) {
                if (b.getTotalPrice() != null) {
                    revenue = revenue.add(b.getTotalPrice());
                }
            }

            if (b.getStatus() != null && b.getStatus().equalsIgnoreCase("cancelled")) {
                cancellations++;
            }

            if (b.getCheckIn() != null && b.getCheckOut() != null && 
                b.getStatus() != null && !b.getStatus().equalsIgnoreCase("cancelled")) {
                occupiedNights += ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            }
        }

        // Tính occupancy cho period
        List<RoomEntity> allRooms = roomRepository.findAll();
        int totalRooms = allRooms.size();
        double occupancy = (totalRooms > 0 && bookings.size() > 0)
            ? (double) occupiedNights / (totalRooms * bookings.size()) * 100.0
            : 0.0;

        return ReportSeriesPoint.builder()
                .date(date)
                .revenue(revenue)
                .bookings(bookingCount)
                .cancellations(cancellations)
                .occupancy(occupancy)
                .build();
    }
}

