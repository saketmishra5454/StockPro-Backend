package com.stockpro.auth.service.impl;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.repository.UserRepository;
import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${stockpro.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${stockpro.mail.from}")
    private String mailFrom;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_MINUTES = 30;


    @Override
    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }

        // Persist only the password hash
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("STAFF");
        }

        return userRepository.save(user);
    }


    @Override
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with email: " + email));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact your administrator.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password.");
        }

        // Last login is an audit field, not part of token state
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
    }


    @Override
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }

    @Override
    public String refreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Refresh token is required.");
        }
        if (!jwtUtil.isTokenValid(token)) {
            throw new RuntimeException("Token is invalid or expired.");
        }

        int userId = jwtUtil.extractUserId(token);
        User user = getUserById(userId);
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact your administrator.");
        }

        return jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Token is required.");
        }
        if (!jwtUtil.isTokenValid(token)) {
            throw new RuntimeException("Token is invalid or expired.");
        }
    }


    @Override
    public User getUserById(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }


    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);

        // Keep role, password, and account status out of profile updates
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


    @Override
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required.");
        }

        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(this::sendPasswordResetEmail);
    }

    @Override
    public void resetPasswordWithToken(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Reset token is required.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }

        User user = userRepository.findByResetPasswordTokenHash(hashToken(token))
                .orElseThrow(() -> new RuntimeException("Password reset link is invalid or expired."));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact your administrator.");
        }

        if (user.getResetPasswordTokenExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getResetPasswordTokenExpiresAt())) {
            clearResetToken(user);
            userRepository.save(user);
            throw new RuntimeException("Password reset link is invalid or expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        clearResetToken(user);
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with email: " + email));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact your administrator.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        clearResetToken(user);
        userRepository.save(user);
    }

    private void sendPasswordResetEmail(User user) {
        String token = generateToken();
        user.setResetPasswordTokenHash(hashToken(token));
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
        userRepository.save(user);

        String resetLink = resetPasswordUrl + "?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Reset your StockPro password");
        message.setText("""
                We received a request to reset your StockPro password.

                Open this link to choose a new password:
                %s

                This link expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(resetLink, RESET_TOKEN_MINUTES));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            clearResetToken(user);
            userRepository.save(user);
            throw new IllegalStateException("Password reset email could not be sent. Check SMTP configuration.", ex);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Password reset token hashing is unavailable.", ex);
        }
    }

    private void clearResetToken(User user) {
        user.setResetPasswordTokenHash(null);
        user.setResetPasswordTokenExpiresAt(null);
    }


    @Override
    public void deactivateUser(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }


    @Override
    public void activateUser(int userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }


    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
