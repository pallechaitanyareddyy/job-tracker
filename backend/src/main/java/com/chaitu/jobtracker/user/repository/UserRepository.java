package com.chaitu.jobtracker.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chaitu.jobtracker.user.entity.User;

/**
 * Spring Data writes the implementation at startup. The method names are the query:
 * findByEmail becomes "select ... from users where email = ?".
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
