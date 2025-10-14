package com.luxestay.hotel.dto;

import com.luxestay.hotel.model.Review;
import com.luxestay.hotel.model.Room;

import java.util.List;
import java.util.Map;

public class RoomDetail {
    private Room room;
    private String floorRange;            // "Tầng 15-20"
    private String description;           // mô tả dài
    private List<String> highlights;      // tag nổi bật
    private List<String> gallery;         // ảnh slide
    private Map<String, List<String>> amenities; // nhóm tiện nghi
    private Map<Integer,Integer> ratingHistogram; // 5->78, 4->32...
    private List<Review> reviews;         // review mẫu

    public Room getRoom(){return room;} public void setRoom(Room room){this.room=room;}
    public String getFloorRange(){return floorRange;} public void setFloorRange(String floorRange){this.floorRange=floorRange;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public List<String> getHighlights(){return highlights;} public void setHighlights(List<String> highlights){this.highlights=highlights;}
    public List<String> getGallery(){return gallery;} public void setGallery(List<String> gallery){this.gallery=gallery;}
    public Map<String, List<String>> getAmenities(){return amenities;} public void setAmenities(Map<String, List<String>> amenities){this.amenities=amenities;}
    public Map<Integer, Integer> getRatingHistogram(){return ratingHistogram;} public void setRatingHistogram(Map<Integer, Integer> ratingHistogram){this.ratingHistogram=ratingHistogram;}
    public List<Review> getReviews(){return reviews;} public void setReviews(List<Review> reviews){this.reviews=reviews;}
}
