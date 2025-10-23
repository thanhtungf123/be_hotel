package com.luxestay.hotel.model;

import com.luxestay.hotel.model.entity.HotelService;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
public class HotelServiceDTO {

    private Long serviceId;
    private String serviceName;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean isAvailable;
    private LocalDateTime createdAt;

    public HotelServiceDTO(HotelService service) {
        this.serviceId = service.getServiceId();
        this.serviceName = service.getServiceName();
        this.description = service.getDescription();
        this.price = service.getPrice();
        this.category = service.getCategory();
        this.isAvailable = service.getIsAvailable();
        this.createdAt = service.getCreatedAt();
    }
}