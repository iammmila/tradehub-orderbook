package com.ab.authservice.config;

import com.ab.authservice.model.Role;
import com.ab.authservice.model.User;
import com.ab.authservice.repository.RoleRepository;
import com.ab.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev") // seed only in dev
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedNormalUser();
    }

    private void seedRoles() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    log.info("Seeding role: ROLE_USER");
                    return roleRepository.save(Role.builder().name("ROLE_USER").build());
                });

        roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    log.info("Seeding role: ROLE_ADMIN");
                    return roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
                });
    }

    private User seedAdminUser() {
        return userRepository.findByUsername("admin")
                .orElseGet(() -> {
                    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                            .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

                    log.info("Seeding admin user: admin");

                    return userRepository.save(User.builder()
                            .username("admin")
                            .firstName("admin")
                            .lastName("admin")
                            .email("admin@tradehub.com")
                            .password(passwordEncoder.encode("admin123"))
                            .role(adminRole)
                            .build());
                });
    }

    private User seedNormalUser() {
        return userRepository.findByUsername("user123")
                .orElseGet(() -> {
                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

                    log.info("Seeding normal user: user");

                    return userRepository.save(User.builder()
                            .username("user123")
                            .firstName("firstname")
                            .lastName("lastname")
                            .email("user123@tradehub.com")
                            .password(passwordEncoder.encode("user123"))
                            .role(userRole)
                            .build());
                });
    }
}