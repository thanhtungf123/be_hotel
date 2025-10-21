package com.luxestay.hotel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String author;
    private String avatar;   // url
    private int stars;       // 1..5
    private String content;
    private String timeAgo;  // "2 tuần trước", "1 tháng trước"

    public Review() {}
    public Review(String author, String avatar, int stars, String content, String timeAgo) {
        this.author = author; this.avatar = avatar; this.stars = stars; this.content = content; this.timeAgo = timeAgo;
    }

    public String getAuthor(){return author;} public void setAuthor(String author){this.author=author;}
    public String getAvatar(){return avatar;} public void setAvatar(String avatar){this.avatar=avatar;}
    public int getStars(){return stars;} public void setStars(int stars){this.stars=stars;}
    public String getContent(){return content;} public void setContent(String content){this.content=content;}
    public String getTimeAgo(){return timeAgo;} public void setTimeAgo(String timeAgo){this.timeAgo=timeAgo;}
}
