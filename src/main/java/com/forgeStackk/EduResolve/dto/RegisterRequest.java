package com.forgeStackk.EduResolve.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    @NotBlank(message = "First name is required")
    private String firstName;
    private String lastName;
    @NotBlank(message = "Username is required")
    private String username;
    private String className;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters and contain at least one letter and one digit"
    )
    private String password;
    /** Ignored from client — public registration always creates a STUDENT account. */
    @JsonIgnore
    private String role;
    private String phoneNumber;
    private String schoolName;
    /** UUID of tp_student — sent by parent at registration to link child. */
    private String studentId;
}
