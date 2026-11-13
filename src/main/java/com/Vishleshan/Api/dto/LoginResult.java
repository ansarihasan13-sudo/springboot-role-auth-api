
package com.Vishleshan.Api.dto;

public class LoginResult {

    private final AuthResponse authResponse;
    private final String refreshToken;

    public LoginResult(
            AuthResponse authResponse,
            String refreshToken) {

        this.authResponse = authResponse;
        this.refreshToken = refreshToken;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
