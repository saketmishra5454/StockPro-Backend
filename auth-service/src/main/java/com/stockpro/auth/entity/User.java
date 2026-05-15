package com.stockpro.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    // Stored as a BCrypt hash; plain text passwords are never persisted
    @Column(nullable = false)
    private String passwordHash;

    private String phone;

    @Column(nullable = false)
    private String role;

    private String department;

    // Soft delete flag - deactivated users cannot login but records are kept
    @Column(nullable = false)
    private boolean isActive = true;

    // Set automatically when user is first created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Updated every time user successfully logs in
    private LocalDateTime lastLoginAt;

    private String resetPasswordTokenHash;

    private LocalDateTime resetPasswordTokenExpiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
}
