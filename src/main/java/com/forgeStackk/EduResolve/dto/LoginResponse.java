package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String role;
    private String className;
    private String phoneNumber;
    private String schoolName;
    private String message;
    private boolean success;
    private Integer studentId;
    private Integer teacherId;
    private Integer adminId;
    private Integer parentId;
    private String token;
}
