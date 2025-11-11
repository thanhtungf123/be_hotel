package com.luxestay.hotel.dto.booking;

import java.util.List;

public class BookingRequest {
    private Long roomId;
    private String checkIn;   // yyyy-MM-dd
    private String checkOut;  // yyyy-MM-dd
    private Integer adults;    // số người lớn (≥1)
    private Integer children;  // số trẻ em (≥0)
    private String note;

     private String paymentChoice;    // "deposit" | "full" (optional, FE dùng để hiển thị, BE vẫn set deposit=30%)
    private Integer depositPercent;  // optional, default 30

    // NEW: Services
    private List<Integer> serviceIds;  // Danh sách ID dịch vụ đã chọn

    // NEW: KYC / Bank info
    private String fullName;
    private String dateOfBirth; // yyyy-MM-dd
    private String gender; // male|female|other
    private String phoneNumber;
    private String nationalIdNumber;
    private String idFrontUrl;
    private String idBackUrl;

    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private String bankCode;
    private String bankBranch;

    public Long getRoomId() { return roomId; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public Integer getAdults() { return adults; }
    public Integer getChildren() { return children; }
    public String getNote() { return note; }
    public String getPaymentChoice() { return paymentChoice; }
    public Integer getDepositPercent() { return depositPercent; }
    public String getFullName() { return fullName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getNationalIdNumber() { return nationalIdNumber; }
    public String getIdFrontUrl() { return idFrontUrl; }
    public String getIdBackUrl() { return idBackUrl; }
    public String getBankAccountName() { return bankAccountName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public String getBankName() { return bankName; }
    public String getBankCode() { return bankCode; }
    public String getBankBranch() { return bankBranch; }
    public List<Integer> getServiceIds() { return serviceIds; }

    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public void setAdults(Integer adults) { this.adults = adults; }
    public void setChildren(Integer children) { this.children = children; }
    public void setNote(String note) { this.note = note; }
    public void setPaymentChoice(String paymentChoice) { this.paymentChoice = paymentChoice; }
    public void setDepositPercent(Integer depositPercent) { this.depositPercent = depositPercent; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(String gender) { this.gender = gender; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setNationalIdNumber(String nationalIdNumber) { this.nationalIdNumber = nationalIdNumber; }
    public void setIdFrontUrl(String idFrontUrl) { this.idFrontUrl = idFrontUrl; }
    public void setIdBackUrl(String idBackUrl) { this.idBackUrl = idBackUrl; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public void setServiceIds(List<Integer> serviceIds) { this.serviceIds = serviceIds; }
}
