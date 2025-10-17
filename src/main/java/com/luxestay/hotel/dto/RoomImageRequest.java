// com.luxestay.hotel.dto.RoomImageRequest
package com.luxestay.hotel.dto;

public class RoomImageRequest {
    private String imageUrl;
    private Boolean primary;   // đặt làm ảnh chính hay không
    private Integer sortOrder; // 0..n

    public String getImageUrl(){return imageUrl;}
    public Boolean getPrimary(){return primary;}
    public Integer getSortOrder(){return sortOrder;}
    public void setImageUrl(String v){imageUrl=v;}
    public void setPrimary(Boolean v){primary=v;}
    public void setSortOrder(Integer v){sortOrder=v;}
}
