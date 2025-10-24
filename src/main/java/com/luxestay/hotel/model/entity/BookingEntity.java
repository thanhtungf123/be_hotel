package com.luxestay.hotel.model.entity;

import com.luxestay.hotel.model.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "ix_bookings_account_status_created", columnList = "account_id, status, created_at")
        })
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomEntity room;

    @Column(name = "check_in_date")
    private LocalDate checkIn;

    @Column(name = "check_out_date")
    private LocalDate checkOut;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "payment_state")
    private String paymentState;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cancel_reason", columnDefinition = "NVARCHAR(MAX)")
    private String cancelReason;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_approved_by")
    private Integer cancelApprovedBy;

    @Column(name = "cancel_approved_at")
    private LocalDateTime cancelApprovedAt;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BookingCustomerDetails customerDetails;

    @Column(name = "payment_reviewed_at")
    private LocalDateTime paymentReviewedAt;
    
    @Column(name = "payment_reviewed_by")
    private Integer paymentReviewedBy;
    
    @Column(name = "payment_note", columnDefinition = "NVARCHAR(MAX)")
    private String paymentNote;

    // getters/setters
    public Integer getId() { return id; }
    public Account getAccount() { return account; }
    public RoomEntity getRoom() { return room; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getCancelRequestedAt() { return cancelRequestedAt; }
    public Integer getCancelApprovedBy() { return cancelApprovedBy; }
    public LocalDateTime getCancelApprovedAt() { return cancelApprovedAt; }
    public BookingCustomerDetails getCustomerDetails() { return customerDetails; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public String getPaymentState() { return paymentState; }
    public LocalDateTime getPaymentReviewedAt() { return paymentReviewedAt; }
    public Integer getPaymentReviewedBy() { return paymentReviewedBy; }
    public String getPaymentNote() { return paymentNote; }

    public void setId(Integer id) { this.id = id; }
    public void setAccount(Account account) { this.account = account; }
    public void setRoom(RoomEntity room) { this.room = room; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setCancelRequestedAt(LocalDateTime t) { this.cancelRequestedAt = t; }
    public void setCancelApprovedBy(Integer id) { this.cancelApprovedBy = id; }
    public void setCancelApprovedAt(LocalDateTime t) { this.cancelApprovedAt = t; }
    public void setCustomerDetails(BookingCustomerDetails customerDetails) { this.customerDetails = customerDetails; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public void setPaymentState(String paymentState) { this.paymentState = paymentState; }
    public void setPaymentReviewedAt(LocalDateTime t) { this.paymentReviewedAt = t; }
    public void setPaymentReviewedBy(Integer id) { this.paymentReviewedBy = id; }
    public void setPaymentNote(String s) { this.paymentNote = s; }
}
