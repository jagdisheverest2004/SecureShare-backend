package org.example.secureshare.repository;

import org.example.secureshare.model.AppRole;
import org.example.secureshare.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(AppRole appRole);

    // Additional query methods can be defined here if needed
}
