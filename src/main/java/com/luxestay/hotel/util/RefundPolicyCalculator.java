package com.luxestay.hotel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Tính toán policy hoàn tiền theo quy định:
 * 
 * ĐẶT CỌC (deposit):
 * - Hủy trước 5 ngày: hoàn 80%
 * - Hủy trước 3 ngày: hoàn 50%
 * - Hủy trước 2 ngày: hoàn 30%
 * - Hủy trước 24h: KHÔNG được hủy
 * 
 * THANH TOÁN TOÀN BỘ (full payment):
 * - Hủy trước 5 ngày: hoàn 100%
 * - Hủy trước 2 ngày: hoàn 70%
 * - Hủy trước 24h: KHÔNG được hủy
 */
public class RefundPolicyCalculator {
    
    public static class RefundResult {
        private final boolean canCancel;
        private final int refundPercent;
        private final BigDecimal refundAmount;
        private final String message;
        
        public RefundResult(boolean canCancel, int refundPercent, BigDecimal refundAmount, String message) {
            this.canCancel = canCancel;
            this.refundPercent = refundPercent;
            this.refundAmount = refundAmount;
            this.message = message;
        }
        
        public boolean isCanCancel() { return canCancel; }
        public int getRefundPercent() { return refundPercent; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public String getMessage() { return message; }
    }
    
    /**
     * Tính toán số tiền hoàn trả
     * 
     * @param checkInDate Ngày check-in
     * @param paidAmount Số tiền đã thanh toán
     * @param isFullPayment true nếu thanh toán toàn bộ, false nếu đặt cọc
     * @return RefundResult chứa thông tin hoàn tiền
     */
    public static RefundResult calculateRefund(LocalDate checkInDate, BigDecimal paidAmount, boolean isFullPayment) {
        if (checkInDate == null || paidAmount == null) {
            return new RefundResult(false, 0, BigDecimal.ZERO, "Thiếu thông tin để tính toán");
        }
        
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);
        
        // Không cho phép hủy trước 24h (1 ngày)
        if (daysUntilCheckIn < 1) {
            return new RefundResult(false, 0, BigDecimal.ZERO, 
                "Không thể hủy trong vòng 24 giờ trước check-in");
        }
        
        int refundPercent = 0;
        
        if (isFullPayment) {
            // THANH TOÁN TOÀN BỘ
            if (daysUntilCheckIn >= 5) {
                refundPercent = 100; // Hoàn 100%
            } else if (daysUntilCheckIn >= 2) {
                refundPercent = 70;  // Hoàn 70%
            } else {
                // < 2 ngày nhưng >= 1 ngày: không được hủy
                return new RefundResult(false, 0, BigDecimal.ZERO,
                    "Thanh toán toàn bộ chỉ được hủy trước 2 ngày");
            }
        } else {
            // ĐẶT CỌC
            if (daysUntilCheckIn >= 5) {
                refundPercent = 80;  // Hoàn 80%
            } else if (daysUntilCheckIn >= 3) {
                refundPercent = 50;  // Hoàn 50%
            } else if (daysUntilCheckIn >= 2) {
                refundPercent = 30;  // Hoàn 30%
            } else {
                // < 2 ngày nhưng >= 1 ngày: không được hủy
                return new RefundResult(false, 0, BigDecimal.ZERO,
                    "Đặt cọc chỉ được hủy trước 2 ngày");
            }
        }
        
        BigDecimal refundAmount = paidAmount
            .multiply(BigDecimal.valueOf(refundPercent))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        
        String message = String.format("Hủy trước %d ngày, hoàn %d%% (%s VNĐ)", 
            daysUntilCheckIn, refundPercent, refundAmount.toPlainString());
        
        return new RefundResult(true, refundPercent, refundAmount, message);
    }
    
    /**
     * Lấy thông tin policy dưới dạng text để hiển thị
     */
    public static String getPolicyText(boolean isFullPayment) {
        if (isFullPayment) {
            return """
                CHÍNH SÁCH HỦY - THANH TOÁN TOÀN BỘ:
                • Hủy trước 5 ngày: Hoàn 100%
                • Hủy trước 2 ngày: Hoàn 70%
                • Hủy trong vòng 24h: KHÔNG được hủy
                """;
        } else {
            return """
                CHÍNH SÁCH HỦY - ĐẶT CỌC:
                • Hủy trước 5 ngày: Hoàn 80%
                • Hủy trước 3 ngày: Hoàn 50%
                • Hủy trước 2 ngày: Hoàn 30%
                • Hủy trong vòng 24h: KHÔNG được hủy
                """;
        }
    }
}

