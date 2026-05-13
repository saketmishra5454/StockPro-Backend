package com.stockpro.auth.service.impl;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.repository.UserRepository;
import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuthServiceImpl - the actual business logic implementation.
 *
 * @RequiredArgsConstructor from Lombok generates a constructor with all
 * final fields - this is constructor injection (preferred over @Autowired).
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // BCryptPasswordEncoder from SecurityConfig
    private final JwtUtil jwtUtil;

    /**
     * Register a new user.
     * Steps:
     * 1. Check if email already exists - throw exception if it does
     * 2. Hash the plain-text password using BCrypt
     * 3. Save user to database
     */
    @Override
    public User register(User user) {
        // Check duplicate email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }

        // Hash the password before saving - NEVER save plain text
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // Default role to STAFF if not provided
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("STAFF");
        }

        return userRepository.save(user);
    }

    /**
     * Login - verify credentials and return a JWT token.
     * Steps:
     * 1. Find user by email - throw if not found
     * 2. Check if account is active
     * 3. Verify password against stored BCrypt hash
     * 4. Update lastLoginAt timestamp
     * 5. Generate and return JWT token
     */
    @Override
    public String login(String email, String password) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with email: " + email));

        // Check if account is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact your administrator.");
        }

        // Verify password - BCrypt compares plain text against the stored hash
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password.");
        }

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token with userId, email, role as claims
        return jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
    }

    /**
     * Validate a JWT token.
     * Returns true if token is valid and not expired.
     * Returns false if token is invalid, expired, or tampered with.
     */
    @Override
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }

    /**
     * Get a user by their ID.
     * Throws RuntimeException if user not found (you can replace with custom exception).
     */
    @Override
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    /**
     * Update user profile - only allows changing safe fields.
     * Email, password, and role cannot be changed here (separate endpoints for those).
     */
    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);

        // Only update allowed profile fields
        if (updatedUser.getFullName() != null) {
            existing.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getPhone() != null) {
            existing.setPhone(updatedUser.getPhone());
        }
        if (updatedUser.getDepartment() != null) {
            existing.setDepartment(updatedUser.getDepartment());
        }

        return userRepository.save(existing);
    }

    /**
     * Change password.
     * Takes plain-text new password, hashes it, then saves.
     */
    @Override
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        // Hash the new password before storing
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Deactivate user - soft delete.
     * Sets isActive = false. The user record stays in the database.
     * The user will not be able to login anymore.
     */
    @Override
    public void deactivateUser(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Activate user - restores access after a soft delete.
     */
    @Override
    public void activateUser(int userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Get all users - typically called by Admin only.
     * The role check (ADMIN) is done in the controller layer.
     */
    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
