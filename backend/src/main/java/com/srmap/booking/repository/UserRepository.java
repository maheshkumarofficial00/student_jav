package com.srmap.booking.repository;

import com.srmap.booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndActiveTrue(User.Role role);
}
