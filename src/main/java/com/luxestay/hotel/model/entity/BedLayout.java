package com.luxestay.hotel.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bed_layouts")
public class BedLayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bed_layout_id")
    private Integer id;

    @Column(name = "layout_name", nullable = false, unique = true)
    private String layoutName;

    @Column(name = "single_beds", nullable = false) private Integer singleBeds;
    @Column(name = "double_beds", nullable = false) private Integer doubleBeds;
    @Column(name = "large_double_beds", nullable = false) private Integer largeDoubleBeds;

    @Column(name = "capacity_hint") private Integer capacityHint;
    @Column(name = "created_at")    private LocalDateTime createdAt;

    // getters/setters
    public Integer getId() { return id; }
    public String getLayoutName() { return layoutName; }
    public Integer getSingleBeds() { return singleBeds; }
    public Integer getDoubleBeds() { return doubleBeds; }
    public Integer getLargeDoubleBeds() { return largeDoubleBeds; }
    public Integer getCapacityHint() { return capacityHint; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Integer id) { this.id = id; }
    public void setLayoutName(String layoutName) { this.layoutName = layoutName; }
    public void setSingleBeds(Integer singleBeds) { this.singleBeds = singleBeds; }
    public void setDoubleBeds(Integer doubleBeds) { this.doubleBeds = doubleBeds; }
    public void setLargeDoubleBeds(Integer largeDoubleBeds) { this.largeDoubleBeds = largeDoubleBeds; }
    public void setCapacityHint(Integer capacityHint) { this.capacityHint = capacityHint; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
