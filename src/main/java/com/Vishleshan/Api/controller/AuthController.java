
package com.Vishleshan.Api.controller;

import com.Vishleshan.Api.dto.AuthResponse;
import com.Vishleshan.Api.dto.LoginResult;
import com.Vishleshan.Api.dto.LoginRequest;
import com.Vishleshan.Api.dto.RegisterRequest;
import com.Vishleshan.Api.entity.AccountType;
import com.Vishleshan.Api.repository.AdminRepo;
import com.Vishleshan.Api.repository.EmployeesRepo;
import com.Vishleshan.Api.repository.UserRepository;
import com.Vishleshan.Api.service.AuthService;
import com.Vishleshan.Api.service.RefreshTokenService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AdminRepo adminRepo;
    private final EmployeesRepo employeesRepo;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;


    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            AdminRepo adminRepo,
            EmployeesRepo employeesRepo) {

        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.adminRepo = adminRepo;
        this.employeesRepo = employeesRepo;
    }


    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        String response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie
                .from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.getAuthResponse());
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(
                    value = "refreshToken",
                    required = false
            ) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            createClearCookie().toString()
                    )
                    .body("Refresh token is missing");
        }

        try {

            LoginResult result = authService.refresh(refreshToken);

            ResponseCookie newRefreshCookie = ResponseCookie
                    .from("refreshToken", result.getRefreshToken())
                    .httpOnly(true)
                    .secure(refreshCookieSecure)
                    .sameSite("Strict")
                    .path("/api/auth")
                    .maxAge(Duration.ofMillis(refreshTokenExpiration))
                    .build();

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            newRefreshCookie.toString()
                    )
                    .body(result.getAuthResponse());

        } catch (IllegalArgumentException |
                 RefreshTokenService.RefreshTokenReuseException ex) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            createClearCookie().toString()
                    )
                    .body("Invalid or expired refresh token");
        }
    }


    // Logout current session
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(
                    value = "refreshToken",
                    required = false
            ) String refreshToken) {

        refreshTokenService.revokeRefreshToken(refreshToken);

        ResponseCookie clearCookie = createClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body("Logged out successfully");
    }


    @PostMapping("/logout-all")
    public ResponseEntity<String> logoutAll(Authentication authentication) {

        String email = authentication.getName();

        var admin = adminRepo.findByEmail(email);

        if (admin.isPresent()) {
            refreshTokenService.revokeAllAccountTokens(
                    admin.get().getId(),
                    AccountType.ADMIN
            );
        } else {
            var employee = employeesRepo.findByEmail(email);

            if (employee.isPresent()) {
                refreshTokenService.revokeAllAccountTokens(
                        employee.get().getId(),
                        AccountType.EMPLOYEE
                );
            } else {
                var user = userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Account not found"
                                )
                        );

                refreshTokenService.revokeAllAccountTokens(
                        user.getId(),
                        AccountType.USER
                );
            }
        }

        ResponseCookie clearCookie = createClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body("Logged out from all devices successfully");
    }


    private ResponseCookie createClearCookie() {

        return ResponseCookie
                .from("refreshToken", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
