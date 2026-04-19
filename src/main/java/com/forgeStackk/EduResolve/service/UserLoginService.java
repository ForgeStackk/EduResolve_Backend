package com.forgeStackk.EduResolve.service;

import com.forgeStackk.EduResolve.dto.LoginRequest;
import com.forgeStackk.EduResolve.dto.LoginResponse;
import com.forgeStackk.EduResolve.dto.RegisterRequest;
import com.forgeStackk.EduResolve.entity.UserLogin;
import com.forgeStackk.EduResolve.repository.UserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserLoginService {

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Register a new user (first-time login)
     */
    public LoginResponse register(RegisterRequest request) {
        LoginResponse response = new LoginResponse();

        // Check if email already exists
        if (userLoginRepository.existsByEmail(request.getEmail())) {
            response.setSuccess(false);
            response.setMessage("Email already registered. Please login instead.");
            return response;
        }

        try {
            // Create new user
            UserLogin user = new UserLogin();
            user.setName(request.getName());
            user.setClassName(request.getClassName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
            user.setRole(request.getRole());
            user.setPhoneNumber(request.getPhoneNumber());

            // Save to database
            UserLogin savedUser = userLoginRepository.save(user);

            // Return success response
            response.setId(savedUser.getId());
            response.setName(savedUser.getName());
            response.setEmail(savedUser.getEmail());
            response.setRole(savedUser.getRole());
            response.setClassName(savedUser.getClassName());
            response.setPhoneNumber(savedUser.getPhoneNumber());
            response.setSuccess(true);
            response.setMessage("Registration successful!");

            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Registration failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Login existing user
     */
    public LoginResponse login(LoginRequest request) {
        LoginResponse response = new LoginResponse();

        try {
            // Find user by email
            Optional<UserLogin> userOptional = userLoginRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Email not found. Please register first.");
                return response;
            }

            UserLogin user = userOptional.get();

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                response.setSuccess(false);
                response.setMessage("Invalid password. Please try again.");
                return response;
            }

            // Login successful
            response.setId(user.getId());
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());
            response.setClassName(user.getClassName());
            response.setPhoneNumber(user.getPhoneNumber());
            response.setSuccess(true);
            response.setMessage("Login successful!");

            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Login failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Get user by email
     */
    public Optional<UserLogin> getUserByEmail(String email) {
        return userLoginRepository.findByEmail(email);
    }
}
