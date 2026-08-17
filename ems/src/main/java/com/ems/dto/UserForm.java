package com.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Form backing object for create/edit user account.
 * Password is validated manually on create (see PasswordPolicy) so the same
 * DTO can serve the edit flow, which never carries a password.
 */
@Data
public class UserForm {

    private Long id;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
             message = "Username must be 3-50 characters: letters, digits, dot, dash or underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /** Only set on create. */
    private String password;

    /** RoleType names selected via checkboxes. */
    private Set<String> roles = new LinkedHashSet<>();

    /** Optional 1:1 link to an employee record. */
    private Long employeeId;

    private boolean enabled = true;
}
