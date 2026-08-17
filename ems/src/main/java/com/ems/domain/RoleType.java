package com.ems.domain;

/**
 * Application roles. Stored as the enum name on {@link Role} and exposed to
 * Spring Security as ROLE_<name> authorities.
 */
public enum RoleType {
    ADMIN,
    HR,
    MANAGER,
    EMPLOYEE
}
