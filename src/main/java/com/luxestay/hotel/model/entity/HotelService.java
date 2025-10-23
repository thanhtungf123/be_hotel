package com.luxestay.hotel.model.entity;

import com.luxestay.hotel.model.HotelServiceDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "serviceId")
@ToString(exclude = "bookingServices")
public class HotelService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Lob
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String category;

    @OneToMany(
            mappedBy = "hotelService",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BookingService> bookingServices = new ArrayList<>();

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    public HotelService(HotelServiceDTO dto) {
        this.serviceName = dto.getServiceName();
        this.description = dto.getDescription();
        this.price = dto.getPrice();
        this.category = dto.getCategory();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isAvailable == null) {
            isAvailable = true;
        }
    }

}