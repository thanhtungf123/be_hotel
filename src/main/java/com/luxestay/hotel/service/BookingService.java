package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;

public interface BookingService {
    BookingResponse create(Integer accountId, BookingRequest req);

    // KH gửi yêu cầu hủy
    void requestCancel(Integer bookingId, Integer accountId, String reason);

    // Admin/Staff duyệt/từ chối
    void decideCancel(Integer bookingId, Integer staffId, boolean approve, String note);

    // lịch sử đặt phòng (của 1 account)
    PagedResponse<BookingSummary> history(Integer accountId, String status, Integer page, Integer size);
}
