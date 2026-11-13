package com.Vishleshan.Api.service;
import com.Vishleshan.Api.entity.Admin;
import com.Vishleshan.Api.entity.Employees;
import com.Vishleshan.Api.entity.User;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.EmployeesRepo;
import com.Vishleshan.Api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AdminRepo adminRepo;
    private final EmployeesRepo employeesRepo;
    private final UserRepository userRepository;

    public AppUserDetailsService(

            AdminRepo adminRepo,
            EmployeesRepo employeesRepo,
            UserRepository userRepository) {

        this.adminRepo = adminRepo;
        this.employeesRepo = employeesRepo;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // 1. Check Admin table
        Admin admin = adminRepo.findByEmail(email).orElse(null);

        if (admin != null) {

            String roleName = admin.getRole().getRoleName();

            return org.springframework.security.core.userdetails.User
                    .withUsername(admin.getEmail())
                    .password(admin.getPasswordHash())
                    .roles(roleName)
                    .build();
        }

        // 2. Check Employee table
        Employees employee = employeesRepo.findByEmail(email).orElse(null);

        if (employee != null) {

            String roleName = employee.getRole().getRoleName();

            return org.springframework.security.core.userdetails.User
                    .withUsername(employee.getEmail())
                    .password(employee.getPasswordHash())
                    .roles(roleName)
                    .disabled(!employee.isActive() || employee.getDeletedAt() != null)
                    .build();
        }

        // 3. Check User table
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {

            String roleName = user.getRole().getRoleName();

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(roleName)
                    .build();
        }

        throw new UsernameNotFoundException(
                "Account not found with email: " + email
        );
    }
}