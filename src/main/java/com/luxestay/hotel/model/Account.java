package com.luxestay.hotel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer id;

    @Column(name = "full_name", columnDefinition = "NVARCHAR(255)")
    private String fullName;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @ManyToOne @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;


    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // inverse side (no join column here)
    @OneToOne(mappedBy = "account")
    @JsonIgnore
//    @JsonBackReference
    private Employee employee;
}
