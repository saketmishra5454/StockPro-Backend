package com.stockpro.auth.service;

import com.stockpro.auth.entity.User;

import java.util.List;

public interface AuthService {

    User register(User user);

    String login(String email, String password);

    boolean validateToken(String token);

    String refreshToken(String token);

    void logout(String token);

    User getUserById(int userId);

    User updateProfile(int userId, User updatedUser);

    void changePassword(int userId, String newPassword);

    void requestPasswordReset(String email);

    void resetPasswordWithToken(String token, String newPassword);

    void resetPassword(String email, String newPassword);

    void deactivateUser(int userId);

    void activateUser(int userId);

    List<User> getAllUsers();
}
