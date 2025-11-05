package com.luxestay.hotel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor @ToString @AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Services {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private int id;
    @Column(name = "service_name", columnDefinition = "NVARCHAR(255)")
    private String nameService;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String description;
    private double price;
}
