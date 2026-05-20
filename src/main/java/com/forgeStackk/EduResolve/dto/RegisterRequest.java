package com.forgeStackk.EduResolve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String firstName;
    private String lastName;
    private String username;
    private String className;
    private String email;
    private String password;
    private String role;
    private String phoneNumber;
    private String schoolName;
    /** UUID of tp_student — sent by parent at registration to link child. */
    private String studentId;
}
