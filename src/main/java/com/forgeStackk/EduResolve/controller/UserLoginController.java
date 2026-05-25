package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.ForgotPasswordRequest;
import com.forgeStackk.EduResolve.dto.ForgotPasswordResponse;
import com.forgeStackk.EduResolve.dto.LoginRequest;
import com.forgeStackk.EduResolve.dto.LoginResponse;
import com.forgeStackk.EduResolve.dto.RegisterRequest;
import com.forgeStackk.EduResolve.dto.ResetPasswordRequest;
import com.forgeStackk.EduResolve.dto.ResetPasswordResponse;
import com.forgeStackk.EduResolve.service.UserLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;

    /**
     * Register endpoint - for first-time login/new user registration
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        LoginResponse response = userLoginService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Login endpoint - for existing users
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userLoginService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Logout is typically handled on the frontend by clearing tokens/session
        return ResponseEntity.ok("Logout successful! Session cleared.");
    }

    /**
     * Forgot password endpoint
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = userLoginService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password endpoint
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = userLoginService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the full local profile for the currently authenticated Keycloak user.
     * Looks up the UserLogin row by the email claim in the Keycloak JWT.
     * GET /api/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<LoginResponse> profile(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = jwtAuth.getToken().getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        LoginResponse response = userLoginService.getLoginResponseByEmail(email);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
