package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String className;
    private String phoneNumber;
    private String message;
    private boolean success;
}
