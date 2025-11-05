package com.luxestay.hotel.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSeriesPoint {
    private LocalDate date;                   // mốc ngày (đầu tuần/tháng quy về một ngày đại diện)
    private BigDecimal revenue;               // doanh thu
    private Integer bookings;                 // số booking
    private Integer cancellations;            // số hủy
    private Double occupancy;                 // công suất (%)
}
