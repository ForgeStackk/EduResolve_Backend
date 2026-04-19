package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.dto.LoginRequest;
import com.forgeStackk.EduResolve.dto.LoginResponse;
import com.forgeStackk.EduResolve.dto.RegisterRequest;
import com.forgeStackk.EduResolve.service.UserLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}
