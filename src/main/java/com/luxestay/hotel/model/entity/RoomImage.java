// com.luxestay.hotel.model.entity.RoomImage
package com.luxestay.hotel.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "room_images")
public class RoomImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id") private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "is_primary", nullable = false) private Boolean isPrimary = false;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;

    // getters/setters
    public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
    public RoomEntity getRoom(){return room;} public void setRoom(RoomEntity room){this.room=room;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String imageUrl){this.imageUrl=imageUrl;}
    public Boolean getIsPrimary(){return isPrimary;} public void setIsPrimary(Boolean isPrimary){this.isPrimary=isPrimary;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer sortOrder){this.sortOrder=sortOrder;}
}
