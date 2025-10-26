package com.luxestay.hotel.dto;

public class RoomRecommendRequest {
    private Long accountId; // User ID để personalized recommendation
    private String type; // "popular", "top_rated", "personalized", "auto"
    private Integer limit; // Số lượng phòng gợi ý (default: 5)

    public RoomRecommendRequest() {
        this.limit = 5;
        this.type = "auto";
    }

    public RoomRecommendRequest(Long accountId, String type, Integer limit) {
        this.accountId = accountId;
        this.type = type != null ? type : "auto";
        this.limit = limit != null && limit > 0 ? limit : 5;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}

