
package com.Vishleshan.Api.service;

import com.Vishleshan.Api.entity.Admin;
import com.Vishleshan.Api.entity.Employees;
import com.Vishleshan.Api.entity.User;
import com.Vishleshan.Api.entity.AccountType;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.EmployeesRepo;
import com.Vishleshan.Api.repository.UserRepository;
import com.Vishleshan.Api.security.AppUserPrincipal;

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

            return new AppUserPrincipal(
                    admin.getId(),
                    admin.getEmail(),
                    admin.getPasswordHash(),
                    AccountType.ADMIN,
                    admin.getRole().getRoleName(),
                    true
            );
        }

        // 2. Check Employee table
        Employees employee = employeesRepo.findByEmail(email).orElse(null);

        if (employee != null) {

            boolean enabled =
                    employee.isActive()
                            && employee.getDeletedAt() == null;

            return new AppUserPrincipal(
                    employee.getId(),
                    employee.getEmail(),
                    employee.getPasswordHash(),
                    AccountType.EMPLOYEE,
                    employee.getRole().getRoleName(),
                    enabled
            );
        }

        // 3. Check User table
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {

            return new AppUserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    AccountType.USER,
                    user.getRole().getRoleName(),
                    true
            );
        }

        throw new UsernameNotFoundException(
                "Account not found with email: " + email
        );
    }
}
