package com.luxestay.hotel.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummary {
    private BigDecimal totalRevenue;          // tổng doanh thu (đơn đã thanh toán)
    private Integer totalBookings;            // tổng số booking trong kỳ
    private Integer cancelledBookings;        // số booking bị hủy
    private Double cancellationRate;          // tỷ lệ hủy (%)
    private Double occupancyRate;             // công suất phòng trung bình (%)
    private BigDecimal avgRevenuePerBooking;  // doanh thu TB/booking
}
