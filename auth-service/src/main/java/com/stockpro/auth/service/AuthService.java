package com.stockpro.auth.service;

import com.stockpro.auth.entity.User;

import java.util.List;

/**
 * AuthService Interface - defines the CONTRACT for what auth-service can do.
 *
 * Why use an interface?
 * The controller (AuthResource) depends on this interface, NOT on AuthServiceImpl.
 * This means you can swap the implementation without changing the controller.
 * It also makes unit testing easier - you can mock this interface.
 */
public interface AuthService {

    // Register a new user - saves to DB with hashed password
    User register(User user);

    // Login - verifies email+password, returns JWT token string if valid
    String login(String email, String password);

    // Validate a JWT token - returns true if valid and not expired
    boolean validateToken(String token);

    // Get a single user by their ID
    User getUserById(int userId);

    // Update profile fields (fullName, phone, department)
    User updateProfile(int userId, User updatedUser);

    // Change password - takes plain text new password, hashes it before saving
    void changePassword(int userId, String newPassword);

    // Soft delete - sets isActive = false, user cannot login anymore
    void deactivateUser(int userId);

    // Restore access - sets isActive = true, user can login again
    void activateUser(int userId);

    // Get all users - Admin only
    List<User> getAllUsers();
}
