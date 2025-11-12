package com.luxestay.hotel.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDTO {
    private Integer bookingId;
    private String invoiceNumber;
    private LocalDateTime issueDate;
    
    // Customer info
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Booking info
    private String roomName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer nights;
    private Integer adults;
    private Integer children;
    
    // Pricing breakdown
    private BigDecimal roomTotal;
    private BigDecimal servicesTotal;
    private List<ServiceItem> services;
    private BigDecimal subtotal;
    private BigDecimal tax;           // 10%
    private BigDecimal serviceFee;    // 5%
    private BigDecimal total;
    
    private String paymentState;
    private BigDecimal paidAmount;
    private BigDecimal depositAmount;
    
    private String checkInCode;
    
    public static class ServiceItem {
        private Integer id;
        private String name;
        private String description;
        private BigDecimal price;
        
        public ServiceItem() {}
        
        public ServiceItem(Integer id, String name, String description, BigDecimal price) {
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
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
    
    // Getters and Setters
    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    
    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }
    
    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }
    
    public BigDecimal getRoomTotal() { return roomTotal; }
    public void setRoomTotal(BigDecimal roomTotal) { this.roomTotal = roomTotal; }
    
    public BigDecimal getServicesTotal() { return servicesTotal; }
    public void setServicesTotal(BigDecimal servicesTotal) { this.servicesTotal = servicesTotal; }
    
    public List<ServiceItem> getServices() { return services; }
    public void setServices(List<ServiceItem> services) { this.services = services; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public String getPaymentState() { return paymentState; }
    public void setPaymentState(String paymentState) { this.paymentState = paymentState; }
    
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    
    public String getCheckInCode() { return checkInCode; }
    public void setCheckInCode(String checkInCode) { this.checkInCode = checkInCode; }
}

