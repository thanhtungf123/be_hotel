package com.luxestay.hotel.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_customer_details")
public class BookingCustomerDetails {
    @Id
    @Column(name = "booking_id")
    private Integer bookingId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "national_id_number")
    private String nationalIdNumber;

    @Column(name = "id_front_url")
    private String idFrontUrl;

    @Column(name = "id_back_url")
    private String idBackUrl;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters/setters
    public BookingEntity getBooking() { return booking; }
    public void setBooking(BookingEntity booking) { this.booking = booking; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getNationalIdNumber() { return nationalIdNumber; }
    public void setNationalIdNumber(String nationalIdNumber) { this.nationalIdNumber = nationalIdNumber; }
    public String getIdFrontUrl() { return idFrontUrl; }
    public void setIdFrontUrl(String idFrontUrl) { this.idFrontUrl = idFrontUrl; }
    public String getIdBackUrl() { return idBackUrl; }
    public void setIdBackUrl(String idBackUrl) { this.idBackUrl = idBackUrl; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
