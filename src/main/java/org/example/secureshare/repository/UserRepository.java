package org.example.secureshare.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.secureshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String userName);

    boolean existsByUsername(@NotBlank @Size(min = 5, max = 20) String username);

    boolean existsByEmail(@NotBlank @Size(max = 50) @Email String email);

    Optional<User> findByEmail(String email);
    // Define query methods here
}
