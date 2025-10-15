package com.luxestay.hotel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import lombok.*;

@Entity @Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer id;

    @Column(name = "role_name", unique = true)
    private String name;

    @Column(name = "description")
    private String description;
}
