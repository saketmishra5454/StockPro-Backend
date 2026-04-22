package com.stockpro.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity - maps to the "users" table in stockpro_auth_db
 *
 * Roles:
 *   STAFF    - warehouse staff, day to day stock operations
 *   MANAGER  - inventory manager, reports, approvals
 *   OFFICER  - purchase officer, POs and suppliers
 *   ADMIN    - full system access
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // Auto-generated primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    // Full display name of the user
    @Column(nullable = false)
    private String fullName;

    // Email is the login identifier - must be unique across all users
    @Column(unique = true, nullable = false)
    private String email;

    // Stored as BCrypt hash - NEVER store plain text passwords
    @Column(nullable = false)
    private String passwordHash;

    private String phone;

    // One of: STAFF, MANAGER, OFFICER, ADMIN
    @Column(nullable = false)
    private String role;

    // Department the user belongs to (e.g. "Warehouse A", "Procurement")
    private String department;

    // Soft delete flag - deactivated users cannot login but records are kept
    @Column(nullable = false)
    private boolean isActive = true;

    // Set automatically when user is first created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Updated every time user successfully logs in
    private LocalDateTime lastLoginAt;

    // Runs before INSERT - sets createdAt automatically
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
}