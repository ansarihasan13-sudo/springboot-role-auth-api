package com.Vishleshan.Api.service;
import com.Vishleshan.Api.repository.RefreshTokenRepository;
import com.Vishleshan.Api.dto.AdminDashboardResponse;
import com.Vishleshan.Api.entity.*;
import com.Vishleshan.Api.repository.AccountEmailRegistryRepository;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.EmployeesRepo;
import com.Vishleshan.Api.repository.RoleRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeesService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminRepo adminRepo;
    private final RefreshTokenService refreshTokenService;

    private final EmployeesRepo employeesRepo;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailRegistryRepository accountEmailRegistryRepository;

    public EmployeesService(
            RefreshTokenRepository refreshTokenRepository,
            AdminRepo adminRepo,
            RefreshTokenService refreshTokenService,
            EmployeesRepo employeesRepo,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AccountEmailRegistryRepository accountEmailRegistryRepository) {
        this.refreshTokenRepository=refreshTokenRepository;
        this.adminRepo=adminRepo;
        this.refreshTokenService=refreshTokenService;
        this.employeesRepo = employeesRepo;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountEmailRegistryRepository = accountEmailRegistryRepository;
    }

    // GET: All Employees
    public List<Employees> getAllEmp() {
        return employeesRepo.findAllByDeletedAtIsNull();
    }


    public AdminDashboardResponse getAdminDashboard() {

        long totalEmployees = employeesRepo.countByDeletedAtIsNull();

        long activeEmployees =
                employeesRepo.countByDeletedAtIsNullAndIsActiveTrue();

        long inactiveEmployees =
                employeesRepo.countByDeletedAtIsNullAndIsActiveFalse();

        return new AdminDashboardResponse(
                totalEmployees,
                activeEmployees,
                inactiveEmployees
        );
    }



    // GET: Employee by ID

        public Employees getEmpById(UUID id) {

            return employeesRepo.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Employee not found with id: " + id
                    ));
        }

    // POST: Create Employee
//    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional
    public Employees createAddEmp(Employees employees) {

        if (employees.getEmail() == null ||
                employees.getEmail().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        String email = employees.getEmail()
                .trim()
                .toLowerCase();

        // Global email uniqueness check
        if (accountEmailRegistryRepository.existsByEmail(email)
                || employeesRepo.existsByEmail(email)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        // Find EMPLOYEE role
        RoleEntity employeeRole = roleRepository
                .findByRoleName("EMPLOYEE")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "EMPLOYEE role not found in database"
                ));

        // Prepare Employee
        employees.setId(null);
        employees.setEmail(email);
        employees.setRole(employeeRole);
        employees.setActive(true);

        // Encode password
        employees.setPasswordHash(
                passwordEncoder.encode(employees.getPasswordHash())
        );

        // Save Employee
        Employees savedEmployee = employeesRepo.save(employees);

        // Save Employee email in global registry
        AccountEmailRegistry registry = new AccountEmailRegistry(
                email,
                "EMPLOYEE",
                savedEmployee.getId()
        );

        accountEmailRegistryRepository.save(registry);

        return savedEmployee;
    }

    // PUT: Update Employee
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Employees updateEmp(UUID id, Employees updatedEmp) {

        Employees existingEmp = getEmpById(id);

        String newEmail = updatedEmp.getEmail()
                .trim()
                .toLowerCase();

        // Check email uniqueness only if email is changing
        if (!existingEmp.getEmail().equalsIgnoreCase(newEmail)) {

            if (accountEmailRegistryRepository.existsByEmail(newEmail)
                    || employeesRepo.existsByEmail(newEmail)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email already registered"
                );
            }

            // Update registry email
            AccountEmailRegistry registry =
                    accountEmailRegistryRepository
                            .findByAccountTypeAndAccountId(
                                    "EMPLOYEE",
                                    existingEmp.getId()
                            )
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Employee email registry entry not found"
                            ));

            accountEmailRegistryRepository.delete(registry);

            registry.setEmail(newEmail);

            accountEmailRegistryRepository.save(registry);
        }

        existingEmp.setName(updatedEmp.getName());
        existingEmp.setEmail(newEmail);
        existingEmp.setDepartment(updatedEmp.getDepartment());

        return employeesRepo.save(existingEmp);
    }

    // PATCH: Update Employee Active Status
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Employees updateEmployeeStatus(UUID id, boolean active) {

        Employees employee = getEmpById(id);

        employee.setActive(active);

        return employeesRepo.save(employee);
    }

    // DELETE: Employee
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteEmp(UUID id) {

        // Employee UUID se employee find karo
        Employees employee = getEmpById(id);

        // Logged-in Admin ka email
        String adminEmail = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // Admin UUID retrieve karo
        Admin admin = adminRepo.findByEmail(adminEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Admin account not found"
                ));

        UUID adminId = admin.getId();
        UUID employeeId = employee.getId();

        // Soft delete
        employee.setActive(false);
        employee.setDeletedAt(LocalDateTime.now());
        employee.setDeletedBy(adminId);

        // Employee ke sabhi refresh tokens revoke
        refreshTokenService.revokeAllAccountTokens(
                employeeId,
                AccountType.EMPLOYEE
        );

        employeesRepo.save(employee);
    }
    // PERMANENT DELETE: Employee
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void hardDeleteEmp(UUID id) {

        // 1. Sirf soft-deleted employee find karo
        Employees employee = employeesRepo
                .findByIdAndDeletedAtIsNotNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Deleted employee not found with id: " + id
                ));

        UUID employeeId = employee.getId();


        refreshTokenRepository.deleteAllByAccountIdAndAccountType(
                employeeId,
                AccountType.EMPLOYEE
        );

        // 3. Email registry entry delete karo
        accountEmailRegistryRepository
                .findByAccountTypeAndAccountId(
                        "EMPLOYEE",
                        employeeId
                )
                .ifPresent(accountEmailRegistryRepository::delete);

        // 4. Employee permanently delete karo
        employeesRepo.delete(employee);
    }
}