package com.stockpro.auth.resource;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AuthResource - REST Controller that exposes all auth endpoints.
 *
 * Base path: /api/auth
 * All endpoints here are accessible through the api-gateway at:
 *   http://localhost:8080/api/auth/...
 *
 * /api/auth/login and /api/auth/register are whitelisted in JwtAuthFilter
 * (they don't need a token because the user doesn't have one yet).
 * All other endpoints require a valid JWT in the Authorization header.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user account.
     *
     * Request body (JSON):
     * {
     *   "fullName": "John Doe",
     *   "email": "john@example.com",
     *   "passwordHash": "plainTextPassword",   <- sent as plain text, hashed in service
     *   "phone": "9876543210",
     *   "role": "STAFF",
     *   "department": "Warehouse A"
     * }
     *
     * Response: saved User object (201 Created)
     */
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User saved = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /api/auth/login
     * Login with email and password, returns JWT token.
     *
     * Request body (JSON):
     * {
     *   "email": "john@example.com",
     *   "password": "plainTextPassword"
     * }
     *
     * Response: { "token": "eyJhbGciOiJIUzI1NiJ9..." }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String email    = request.get("email");
        String password = request.get("password");

        String token = authService.login(email, password);
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * POST /api/auth/validate
     * Validate a JWT token - used by other microservices to verify tokens.
     *
     * Request body (JSON):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9..."
     * }
     *
     * Response: { "valid": true } or { "valid": false }
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestBody Map<String, String> request) {
        boolean isValid = authService.validateToken(request.get("token"));
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    /**
     * GET /api/auth/users
     * Get all users - Admin only.
     * (Role check can be added here or enforced via api-gateway in future)
     *
     * Response: List of all User objects
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * GET /api/auth/profile/{id}
     * Get a single user's profile by their userId.
     *
     * Path variable: id = userId (integer)
     * Response: User object
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<User> getProfile(@PathVariable int id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    /**
     * PUT /api/auth/profile/{id}
     * Update profile information (fullName, phone, department).
     *
     * Request body (JSON) - only include fields you want to update:
     * {
     *   "fullName": "John Updated",
     *   "phone": "1234567890",
     *   "department": "Warehouse B"
     * }
     *
     * Response: updated User object
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable int id, @RequestBody User user) {
        return ResponseEntity.ok(authService.updateProfile(id, user));
    }

    /**
     * PUT /api/auth/password/{id}
     * Change a user's password.
     *
     * Request body (JSON):
     * {
     *   "newPassword": "myNewSecurePassword123"
     * }
     *
     * Response: { "message": "Password updated successfully" }
     */
    @PutMapping("/password/{id}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable int id,
            @RequestBody Map<String, String> request) {

        authService.changePassword(id, request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    /**
     * PUT /api/auth/deactivate/{id}
     * Deactivate a user account (soft delete - sets isActive = false).
     * The user record stays in the database but they cannot login.
     *
     * Response: { "message": "User deactivated successfully" }
     */
    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable int id) {
        authService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }
}