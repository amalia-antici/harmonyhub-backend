package com.amalia.harmonyhub_backend.utils;

import com.amalia.harmonyhub_backend.model.Permission;
import com.amalia.harmonyhub_backend.model.Role;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.PermissionRepository;
import com.amalia.harmonyhub_backend.repository.RoleRepository;
import com.amalia.harmonyhub_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class SetupDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Permission deleteAny = createPermissionIfNotFound("DELETE_ANY_EVENT");
        Permission readOnly = createPermissionIfNotFound("READ_PRIVILEGE");

        createRoleIfNotFound("ROLE_ADMIN", List.of(deleteAny, readOnly));
        createRoleIfNotFound("ROLE_USER", List.of(readOnly));

        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setEmail("amaliaantici320@gmail.com");
            admin.setRoles(List.of(roleRepository.findByName("ROLE_ADMIN")));

            admin.setSecurityQuestion("What city were you born in?");

            String rawAnswer = "Pildesti";
            String normalizedAnswer = rawAnswer.toLowerCase().trim();
            admin.setSecurityAnswer(passwordEncoder.encode(normalizedAnswer));

            userRepository.save(admin);
            System.out.println(">>> System Seeder: Default administrative account successfully provisioned with structural multi-factor fallback variables.");
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        Permission p = permissionRepository.findByName(name);
        if (p == null) { p = new Permission(name); permissionRepository.save(p); }
        return p;
    }

    private void createRoleIfNotFound(String name, Collection<Permission> perms) {
        Role r = roleRepository.findByName(name);
        if (r == null) {
            r = new Role(name);
            r.setPermissions(perms);
            roleRepository.save(r);
        }
    }
}
