// com.luxestay.hotel.dto.booking.BookingSummary
package com.luxestay.hotel.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BookingSummary {
    private Integer id;
    private Integer roomId;
    private String  roomName;
    private String  roomImageUrl; // ảnh chính của phòng
    private String  bedLayout;    // ví dụ: "1 giường đôi"
    private Integer guests;       // tổng số khách (adults + children) - backward compatible
    private Integer adults;       // số người lớn
    private Integer children;     // số trẻ em
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Long nights;          // số đêm
    private BigDecimal totalPrice;
    private String status;
    private String cancelReason;
    private String paymentState;           // unpaid|deposit_paid|paid_in_full
    private BigDecimal depositAmount;      // số tiền cọc
    private BigDecimal amountPaid;         // đã thanh toán
    private BigDecimal amountRemaining;    // số tiền còn lại
    private String checkInCode;           // mã check-in
    
    // Refund information
    private String refundAccountHolder;    // Chủ TK ngân hàng
    private String refundAccountNumber;    // STK ngân hàng
    private String refundBankName;         // Tên ngân hàng
    private Boolean refundSubmitted;       // Đã gửi thông tin hoàn tiền
    private Boolean refundCompleted;       // Đã hoàn tiền
    
    // Services selected for this booking
    private List<ServiceInfo> services;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomImageUrl() { return roomImageUrl; }
    public void setRoomImageUrl(String roomImageUrl) { this.roomImageUrl = roomImageUrl; }

    public String getBedLayout() { return bedLayout; }
    public void setBedLayout(String bedLayout) { this.bedLayout = bedLayout; }

    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }
    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public Long getNights() { return nights; }
    public void setNights(Long nights) { this.nights = nights; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    
    public String getPaymentState() { return paymentState; }
    public void setPaymentState(String paymentState) { this.paymentState = paymentState; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    
    public BigDecimal getAmountRemaining() { return amountRemaining; }
    public void setAmountRemaining(BigDecimal amountRemaining) { this.amountRemaining = amountRemaining; }
    
    public String getCheckInCode() { return checkInCode; }
    public void setCheckInCode(String checkInCode) { this.checkInCode = checkInCode; }
    
    // Refund getters/setters
    public String getRefundAccountHolder() { return refundAccountHolder; }
    public void setRefundAccountHolder(String refundAccountHolder) { this.refundAccountHolder = refundAccountHolder; }
    
    public String getRefundAccountNumber() { return refundAccountNumber; }
    public void setRefundAccountNumber(String refundAccountNumber) { this.refundAccountNumber = refundAccountNumber; }
    
    public String getRefundBankName() { return refundBankName; }
    public void setRefundBankName(String refundBankName) { this.refundBankName = refundBankName; }
    
    public Boolean getRefundSubmitted() { return refundSubmitted; }
    public void setRefundSubmitted(Boolean refundSubmitted) { this.refundSubmitted = refundSubmitted; }
    
    public Boolean getRefundCompleted() { return refundCompleted; }
    public void setRefundCompleted(Boolean refundCompleted) { this.refundCompleted = refundCompleted; }
    
    public List<ServiceInfo> getServices() { return services; }
    public void setServices(List<ServiceInfo> services) { this.services = services; }
    
    // Inner class for service info
    public static class ServiceInfo {
        private Integer id;
        private String name;
        private String description;
        private Double price;
        
        public ServiceInfo() {}
        
        public ServiceInfo(Integer id, String name, String description, Double price) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}
