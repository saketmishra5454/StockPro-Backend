package com.stockpro.auth.service.impl;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.repository.UserRepository;
import com.stockpro.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerHashesPasswordDefaultsRoleAndSavesUser() {
        User user = user("staff@stockpro.test", "plain-password");
        user.setRole(null);

        when(userRepository.existsByEmail("staff@stockpro.test")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(user)).thenReturn(user);

        User saved = authService.register(user);

        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getRole()).isEqualTo("STAFF");
        verify(userRepository).save(user);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        User user = user("staff@stockpro.test", "plain-password");

        when(userRepository.existsByEmail("staff@stockpro.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("staff@stockpro.test");
        verify(passwordEncoder, never()).encode(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUpdatesLastLoginAndReturnsJwtToken() {
        User user = user("staff@stockpro.test", "hashed-password");
        user.setUserId(7);
        user.setRole("MANAGER");
        user.setActive(true);

        when(userRepository.findByEmail("staff@stockpro.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken(7, "staff@stockpro.test", "MANAGER")).thenReturn("jwt-token");

        String token = authService.login("staff@stockpro.test", "plain-password");

        assertThat(token).isEqualTo("jwt-token");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void loginRejectsMissingUser() {
        when(userRepository.findByEmail("missing@stockpro.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("missing@stockpro.test", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No account found");
    }

    @Test
    void loginRejectsInactiveUser() {
        User user = user("staff@stockpro.test", "hashed-password");
        user.setActive(false);

        when(userRepository.findByEmail("staff@stockpro.test")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("staff@stockpro.test", "plain-password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
        verify(passwordEncoder, never()).matches(any(String.class), any(String.class));
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = user("staff@stockpro.test", "hashed-password");
        user.setActive(true);

        when(userRepository.findByEmail("staff@stockpro.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("staff@stockpro.test", "wrong-password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Incorrect password");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateTokenDelegatesToJwtUtil() {
        when(jwtUtil.isTokenValid("jwt-token")).thenReturn(true);

        assertThat(authService.validateToken("jwt-token")).isTrue();
    }

    @Test
    void updateProfileOnlyChangesAllowedFields() {
        User existing = user("staff@stockpro.test", "hashed-password");
        existing.setUserId(7);
        existing.setRole("STAFF");

        User update = new User();
        update.setFullName("Updated User");
        update.setPhone("8888888888");
        update.setDepartment("Warehouse B");
        update.setRole("ADMIN");
        update.setEmail("new@stockpro.test");

        when(userRepository.findById(7)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User saved = authService.updateProfile(7, update);

        assertThat(saved.getFullName()).isEqualTo("Updated User");
        assertThat(saved.getPhone()).isEqualTo("8888888888");
        assertThat(saved.getDepartment()).isEqualTo("Warehouse B");
        assertThat(saved.getRole()).isEqualTo("STAFF");
        assertThat(saved.getEmail()).isEqualTo("staff@stockpro.test");
    }

    @Test
    void changePasswordHashesNewPassword() {
        User user = user("staff@stockpro.test", "old-hash");

        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        authService.changePassword(7, "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUserMarksUserInactive() {
        User user = user("staff@stockpro.test", "hashed-password");

        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        authService.deactivateUser(7);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void getAllUsersDelegatesToRepository() {
        User user = user("staff@stockpro.test", "hashed-password");

        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(authService.getAllUsers()).containsExactly(user);
    }

    private User user(String email, String passwordHash) {
        User user = new User();
        user.setFullName("StockPro User");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setPhone("9999999999");
        user.setRole("STAFF");
        user.setDepartment("Warehouse A");
        user.setActive(true);
        return user;
    }
}
