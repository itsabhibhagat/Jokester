package com.application.jokester.auth.controller;

import com.application.jokester.auth.dto.AuthResponse;
import com.application.jokester.auth.dto.LoginRequest;
import com.application.jokester.auth.dto.RegisterRequest;
import com.application.jokester.auth.service.AuthService;
import com.application.jokester.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    // Registers a new user account.
    // The username and email must both be unique in the system.
    // The password is stored as a BCrypt hash — never in plain text.
    // On success, a JWT token is returned so the user can start making requests immediately.
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. Username and email must be unique. Returns a JWT token on success."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    // Logs in an existing user by verifying their username and password.
    // Spring Security handles the actual password verification against the BCrypt hash.
    // On success, a new JWT token is returned which the client must include
    // in the Authorization header of all subsequent protected requests.
    @Operation(
            summary = "Login",
            description = "Authenticates an existing user. Returns a JWT token that must be included as 'Bearer <token>' in the Authorization header for protected endpoints."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response));
    }
}