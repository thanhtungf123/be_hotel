package com.luxestay.hotel.dto.booking;

public class RoomVerificationRequest {
    private Boolean isRoomReady;  // true = phòng OK, false = có vấn đề
    private String issueDescription;  // Mô tả vấn đề nếu có
    
    public RoomVerificationRequest() {}
    
    public RoomVerificationRequest(Boolean isRoomReady, String issueDescription) {
        this.isRoomReady = isRoomReady;
        this.issueDescription = issueDescription;
    }
    
    public Boolean getIsRoomReady() {
        return isRoomReady;
    }
    
    public void setIsRoomReady(Boolean isRoomReady) {
        this.isRoomReady = isRoomReady;
    }
    
    public String getIssueDescription() {
        return issueDescription;
    }
    
    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }
}

