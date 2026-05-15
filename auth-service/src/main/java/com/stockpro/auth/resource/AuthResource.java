package com.stockpro.auth.resource;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User saved = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String email    = request.get("email");
        String password = request.get("password");

        String token = authService.login(email, password);
        return ResponseEntity.ok(Map.of("token", token));
    }


    @PostMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestBody Map<String, String> request) {
        boolean isValid = authService.validateToken(request.get("token"));
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshedToken = authService.refreshToken(request.get("token"));
        return ResponseEntity.ok(Map.of("token", refreshedToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request) {
        authService.logout(request.get("token"));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }


    @GetMapping("/profile/{id}")
    public ResponseEntity<User> getProfile(@PathVariable int id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }


    @PutMapping("/profile/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable int id, @RequestBody User user) {
        return ResponseEntity.ok(authService.updateProfile(id, user));
    }


    @PutMapping("/password/{id}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable int id,
            @RequestBody Map<String, String> request) {

        authService.changePassword(id, request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (newPassword == null || newPassword.isBlank()) {
            authService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "Password reset instructions sent if the account exists"));
        }

        authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPasswordWithToken(@RequestBody Map<String, String> request) {
        authService.resetPasswordWithToken(request.get("token"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }


    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable int id) {
        authService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }


    @PutMapping("/activate/{id}")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable int id) {
        authService.activateUser(id);
        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
