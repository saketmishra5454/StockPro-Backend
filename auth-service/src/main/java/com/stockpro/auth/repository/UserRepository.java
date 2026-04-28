package com.stockpro.auth.repository;

import com.stockpro.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Spring Data JPA generates all SQL automatically.
 * You never write SQL here - just declare method names and Spring figures out the query.
 *
 * How Spring derives queries from method names:
 *   findByEmail        → SELECT * FROM users WHERE email = ?
 *   existsByEmail      → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *   findAllByRole      → SELECT * FROM users WHERE role = ?
 *   findByDepartment   → SELECT * FROM users WHERE department = ?
 *   findByIsActive     → SELECT * FROM users WHERE is_active = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Used during login to find user by their email address
    Optional<User> findByEmail(String email);

    // Used during registration to check if email is already taken
    boolean existsByEmail(String email);

    // Used by Admin to list all users with a specific role
    List<User> findAllByRole(String role);

    // Used to filter users by their department
    List<User> findByDepartment(String department);

    // Used to get all active users (isActive = true) or inactive (isActive = false)
    List<User> findByIsActive(boolean isActive);
}