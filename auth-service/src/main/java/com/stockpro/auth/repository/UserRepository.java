package com.stockpro.auth.repository;

import com.stockpro.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByResetPasswordTokenHash(String resetPasswordTokenHash);

    boolean existsByEmail(String email);

    List<User> findAllByRole(String role);

    List<User> findByDepartment(String department);

    List<User> findByIsActive(boolean isActive);
}
