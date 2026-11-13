package com.Vishleshan.Api.config;

import com.Vishleshan.Api.entity.Admin;
import com.Vishleshan.Api.entity.RoleEntity;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.RoleRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    public CommandLineRunner createAdmin(
            AdminRepo adminRepo,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {

        return args -> {

            System.out.println("===== ADMIN SEEDER STARTED =====");

            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                System.out.println(
                        "Admin creation skipped: email or password is empty."
                );
                return;
            }

            if (adminRepo.existsByEmail(adminEmail)) {
                System.out.println(
                        "Admin creation skipped: email already exists."
                );
                return;
            }

            // Find ADMIN role from roles table
            RoleEntity adminRole = roleRepository
                    .findByRoleName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "ADMIN role not found in database"
                    ));

            // Create Admin entity
            Admin admin = new Admin();

            admin.setName("System Admin");
            admin.setEmail(adminEmail);

            admin.setPasswordHash(
                    passwordEncoder.encode(adminPassword)
            );

            admin.setRole(adminRole);

            adminRepo.save(admin);

            System.out.println(
                    "Admin created successfully: " + adminEmail
            );

            System.out.println("===== ADMIN SEEDER FINISHED =====");
        };
    }
}