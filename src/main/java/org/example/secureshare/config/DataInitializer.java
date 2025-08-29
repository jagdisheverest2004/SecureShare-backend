package org.example.secureshare.config;

import jakarta.annotation.PostConstruct;
import org.example.secureshare.model.AppRole;
import org.example.secureshare.model.Role;
import org.example.secureshare.repository.RoleRepository;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void init() {

        if (roleRepository.findByRoleName(AppRole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_USER));
        }
    }
}