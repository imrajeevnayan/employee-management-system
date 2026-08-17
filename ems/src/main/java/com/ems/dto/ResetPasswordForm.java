package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** An administrator resetting another user's password. */
@Data
public class ResetPasswordForm {

    @NotBlank(message = "New password is required")
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    private String password;

    @NotBlank(message = "Please confirm the new password")
    private String confirmPassword;
}
