package com.cupk.auth.controller;

import com.cupk.auth.dto.AuthLoginRequest;
import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthRegisterRequest;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides lightweight account endpoints for the front-end login flow.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthUserResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/onboarding")
    public AuthUserResponse saveOnboarding(@RequestBody AuthOnboardingRequest request) {
        return authService.saveOnboarding(request);
    }
}
