package com.luxestay.hotel.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor @ToString @AllArgsConstructor
public class Services {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private int id;
    @Column(name = "service_name")
    private String nameService;
    private String description;
    private double price;
}
