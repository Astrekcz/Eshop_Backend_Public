package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private Long userID;

    private String firstName;

    private String lastName;

    private String password;

    private String email;

    private String phoneNumber;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;   // defaultně povolený účet

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;   // vyplní se automaticky při INSERTu

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;   // vyplní/aktualizuje se při UPDATE
}
