package com.luxestay.hotel.model.entity;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Services;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "adults")
    private Integer adults;

    @Column(name = "children")
    private Integer children;

    @Column(name = "check_in_code", length = 20)
    private String checkInCode;

    // ✅ Refund information fields (from tung-request)
    @Column(name = "refund_account_holder", length = 255)
    private String refundAccountHolder;

    @Column(name = "refund_account_number", length = 50)
    private String refundAccountNumber;

    @Column(name = "refund_bank_name", length = 255)
    private String refundBankName;

    @Column(name = "refund_submitted_at")
    private LocalDateTime refundSubmittedAt;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    @Column(name = "refund_completed_by")
    private Integer refundCompletedBy;

    // ✅ Many-to-Many relationship with Services
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "booking_services",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<Services> services = new HashSet<>();

    // === GETTERS ===
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
    public String getCheckInCode() { return checkInCode; }
    public Integer getAdults() { return adults; }
    public Integer getChildren() { return children; }

    // ✅ Refund getters
    public String getRefundAccountHolder() { return refundAccountHolder; }
    public String getRefundAccountNumber() { return refundAccountNumber; }
    public String getRefundBankName() { return refundBankName; }
    public LocalDateTime getRefundSubmittedAt() { return refundSubmittedAt; }
    public LocalDateTime getRefundCompletedAt() { return refundCompletedAt; }
    public Integer getRefundCompletedBy() { return refundCompletedBy; }
    public Set<Services> getServices() { return services; }

    // === SETTERS ===
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
    public void setAdults(Integer adults) { this.adults = adults; }
    public void setChildren(Integer children) { this.children = children; }
    public void setCheckInCode(String checkInCode) { this.checkInCode = checkInCode; }

    // ✅ Refund setters
    public void setRefundAccountHolder(String refundAccountHolder) { this.refundAccountHolder = refundAccountHolder; }
    public void setRefundAccountNumber(String refundAccountNumber) { this.refundAccountNumber = refundAccountNumber; }
    public void setRefundBankName(String refundBankName) { this.refundBankName = refundBankName; }
    public void setRefundSubmittedAt(LocalDateTime refundSubmittedAt) { this.refundSubmittedAt = refundSubmittedAt; }
    public void setRefundCompletedAt(LocalDateTime refundCompletedAt) { this.refundCompletedAt = refundCompletedAt; }
    public void setRefundCompletedBy(Integer refundCompletedBy) { this.refundCompletedBy = refundCompletedBy; }
    public void setServices(Set<Services> services) { this.services = services; }
}
