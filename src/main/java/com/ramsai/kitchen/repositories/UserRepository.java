package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    List<User> findByRole(User.UserRole role);
    
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE u.role = 'GUEST' AND u.createdAt < :threshold"
    )
    List<User> findInactiveGuests(@org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold);
}
