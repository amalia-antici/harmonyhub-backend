package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    User findByPasswordResetToken(String token);
    List<User> findAllByOrderByUsernameAsc();
}

