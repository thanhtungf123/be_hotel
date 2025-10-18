package com.luxestay.hotel.dto.booking;

public class CancelDecisionRequest {
    private Boolean approve;  // true = duyệt, false = từ chối
    private String note;      // ghi chú của staff (optional)

    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
