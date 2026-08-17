package com.ems.dto;

/** Shared password strength rule used by forms and services. */
public final class PasswordPolicy {

    public static final String PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,64}$";

    public static final String MESSAGE =
            "Password must be 8-64 characters and include an uppercase letter, a lowercase letter, "
                    + "a digit and a special character.";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(PATTERN);
    }
}
