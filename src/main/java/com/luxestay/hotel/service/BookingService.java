package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import org.springframework.transaction.annotation.Transactional;

public interface BookingService {
    BookingResponse create(Integer accountId, BookingRequest req);

    // KH gửi yêu cầu hủy
    void requestCancel(Integer bookingId, Integer accountId, String reason);

    // Admin/Staff duyệt/từ chối
    void decideCancel(Integer bookingId, Integer staffId, boolean approve, String note);

    // lịch sử đặt phòng (của 1 account)
    PagedResponse<BookingSummary> history(Integer accountId, String status, Integer page, Integer size);

    @Transactional
        // Đảm bảo cả 2 cùng thành công
    void confirmBookingPayment(int bookingId);

    @Transactional
    void onPaymentCaptured(int bookingId);

    // Customer submit refund info
    void submitRefundInfo(Integer bookingId, Integer accountId, RefundInfoRequest request);

    // Staff confirm refund completed
    void confirmRefundCompleted(Integer bookingId, Integer staffId);

}
