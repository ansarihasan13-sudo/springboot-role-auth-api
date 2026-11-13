
package com.Vishleshan.Api.service;

import com.Vishleshan.Api.dto.LoginResult;
import com.Vishleshan.Api.dto.AuthResponse;
import com.Vishleshan.Api.dto.LoginRequest;
import com.Vishleshan.Api.dto.RegisterRequest;

import com.Vishleshan.Api.entity.*;

import com.Vishleshan.Api.repository.AccountEmailRegistryRepository;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.EmployeesRepo;
import com.Vishleshan.Api.repository.RoleRepository;
import com.Vishleshan.Api.repository.UserRepository;

import com.Vishleshan.Api.security.JwtService;
import com.Vishleshan.Api.util.EmailNormalizer;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final AppUserDetailsService appUserDetailsService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminRepo adminRepo;
    private final EmployeesRepo employeesRepo;
    private final AccountEmailRegistryRepository accountEmailRegistryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AppUserDetailsService appUserDetailsService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminRepo adminRepo,
            EmployeesRepo employeesRepo,
            AccountEmailRegistryRepository accountEmailRegistryRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.appUserDetailsService = appUserDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.adminRepo = adminRepo;
        this.employeesRepo = employeesRepo;
        this.accountEmailRegistryRepository = accountEmailRegistryRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Public registration: only normal USER
    @Transactional
    public String register(RegisterRequest request) {

        if (request.getRole() != null &&
                !request.getRole().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role cannot be specified during registration"
            );
        }

        String email = EmailNormalizer.normalize(
                request.getEmail()
        );

        // Global email uniqueness check
        if (accountEmailRegistryRepository.existsByEmail(email)
                || adminRepo.existsByEmail(email)
                || employeesRepo.existsByEmail(email)
                || userRepository.existsByEmail(email)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        RoleEntity userRole = roleRepository
                .findByRoleName("USER")
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "USER role not found in database"
                        )
                );

        User user = new User();

        user.setEmail(email);

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(userRole);

        User savedUser = userRepository.save(user);

        AccountEmailRegistry registry =
                new AccountEmailRegistry(
                        email,
                        "USER",
                        savedUser.getId()
                );

        accountEmailRegistryRepository.save(registry);

        return "User registered successfully";
    }

    // Login for ADMIN, EMPLOYEE and USER
    public LoginResult login(LoginRequest request) {

        String email = EmailNormalizer.normalize(
                request.getEmail()
        );

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        UUID accountId;
        AccountType accountType;
        String role;

        // 1. ADMIN
        var admin = adminRepo.findByEmail(email);

        if (admin.isPresent()) {

            accountId = admin.get().getId();
            accountType = AccountType.ADMIN;
            role = admin.get().getRole().getRoleName();

        } else {

            // 2. EMPLOYEE
            var employee = employeesRepo.findByEmail(email);

            if (employee.isPresent()) {

                accountId = employee.get().getId();
                accountType = AccountType.EMPLOYEE;
                role = employee.get().getRole().getRoleName();

            } else {

                // 3. USER
                var user = userRepository.findByEmail(email);

                if (user.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Account not found"
                    );
                }

                accountId = user.get().getId();
                accountType = AccountType.USER;
                role = user.get().getRole().getRoleName();
            }
        }

        String accessToken =
                jwtService.generateToken(userDetails);

        String refreshToken =
                refreshTokenService.createRefreshToken(
                        accountId,
                        accountType
                );

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                email,
                role
        );

        return new LoginResult(
                authResponse,
                refreshToken
        );
    }

    // Refresh access token and rotate refresh token
    public LoginResult refresh(String rawRefreshToken) {

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(
                        rawRefreshToken
                );

        try {

            String email = switch (result.accountType()) {

                case ADMIN -> adminRepo.findById(result.accountId())
                        .map(Admin::getEmail)
                        .orElseThrow(() -> new IllegalStateException(
                                "Account not found"
                        ));

                case EMPLOYEE -> employeesRepo.findById(result.accountId())
                        .map(Employees::getEmail)
                        .orElseThrow(() -> new IllegalStateException(
                                "Account not found"
                        ));

                case USER -> userRepository.findById(result.accountId())
                        .map(User::getEmail)
                        .orElseThrow(() -> new IllegalStateException(
                                "Account not found"
                        ));
            };

            UserDetails userDetails =
                    appUserDetailsService.loadUserByUsername(email);

            if (!userDetails.isEnabled()) {

                throw new IllegalStateException(
                        "Account is disabled"
                );
            }

            String accessToken =
                    jwtService.generateToken(userDetails);

            String role = userDetails.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority();

            AuthResponse authResponse =
                    new AuthResponse(
                            accessToken,
                            email,
                            role
                    );

            return new LoginResult(
                    authResponse,
                    result.refreshToken()
            );

        } catch (RuntimeException exception) {

            refreshTokenService.revokeAllAccountTokens(
                    result.accountId(),
                    result.accountType()
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unable to refresh session"
            );
        }
    }
}
