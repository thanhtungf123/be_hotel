package com.luxestay.hotel.dto.booking;

public class BookingResponse {
    private Integer bookingId;
    private String status;
    private Integer totalVnd;
    private Integer depositVnd;   // NEW
    private String paymentState;

    public BookingResponse(Integer bookingId, String status, Integer totalVnd, Integer depositVnd, String paymentState) {
        this.bookingId = bookingId;
        this.status = status;
        this.totalVnd = totalVnd;
        this.depositVnd = depositVnd;
        this.paymentState = paymentState;
    }

    public Integer getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public Integer getTotalVnd() { return totalVnd; }
    public Integer getDepositVnd() { return depositVnd; }
    public String getPaymentState() { return paymentState; }
}
