package com.luxestay.hotel.dto.booking;

public class RefundInfoRequest {
    private String accountHolder;    // Chủ TK ngân hàng
    private String accountNumber;    // STK ngân hàng
    private String bankName;         // Tên ngân hàng

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}

