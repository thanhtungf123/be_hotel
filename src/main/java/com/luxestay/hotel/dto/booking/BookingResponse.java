package com.luxestay.hotel.dto.booking;

import java.util.List;
import java.util.Map;

public class BookingResponse {
    private Integer bookingId;
    private String status;
    private Integer totalVnd;
    private Integer subtotalVnd;
    private Integer taxVnd;
    private Integer serviceFeeVnd;
    private Integer depositVnd;
    private String paymentState;
    private List<Map<String, Object>> services;

    public BookingResponse(Integer bookingId, String status, Integer totalVnd, Integer subtotalVnd, 
                          Integer taxVnd, Integer serviceFeeVnd, Integer depositVnd, 
                          String paymentState, List<Map<String, Object>> services) {
        this.bookingId = bookingId;
        this.status = status;
        this.totalVnd = totalVnd;
        this.subtotalVnd = subtotalVnd;
        this.taxVnd = taxVnd;
        this.serviceFeeVnd = serviceFeeVnd;
        this.depositVnd = depositVnd;
        this.paymentState = paymentState;
        this.services = services;
    }

    public Integer getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public Integer getTotalVnd() { return totalVnd; }
    public Integer getSubtotalVnd() { return subtotalVnd; }
    public Integer getTaxVnd() { return taxVnd; }
    public Integer getServiceFeeVnd() { return serviceFeeVnd; }
    public Integer getDepositVnd() { return depositVnd; }
    public String getPaymentState() { return paymentState; }
    public List<Map<String, Object>> getServices() { return services; }
}
