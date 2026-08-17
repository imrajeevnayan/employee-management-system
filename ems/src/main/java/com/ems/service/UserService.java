package com.ems.service;

import com.ems.domain.Employee;
import com.ems.domain.Role;
import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.dto.UserForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.RoleRepository;
import com.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    /** Resolves the authenticated principal from Spring Security to a fully loaded User. */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UsernameNotFoundException("No authenticated user in the current session");
        }
        return userRepository.findWithRolesAndEmployeeByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user no longer exists: " + authentication.getName()));
    }

    /** Nullable variant for model attributes rendered on pages also visible to anonymous users. */
    public User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (UsernameNotFoundException ex) {
            return null;
        }
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<User> listAll() {
        return userRepository.findAllWithRolesAndEmployeeByOrderByUsernameAsc();
    }

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Transactional
    public User create(UserForm form) {
        if (userRepository.existsByUsernameIgnoreCase(form.getUsername())) {
            throw new DuplicateResourceException("Username '" + form.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateResourceException("Email '" + form.getEmail() + "' is already in use");
        }
        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setEnabled(form.isEnabled());
        user.setRoles(resolveRoles(form.getRoles()));
        userRepository.save(user);
        linkEmployee(user, form.getEmployeeId());
        return user;
    }

    /**
     * Updates email, roles, enabled flag and employee link. Username is immutable.
     * Guards against an administrator locking themselves out.
     */
    @Transactional
    public User update(Long id, UserForm form, User current) {
        User user = findById(id);
        if (user.getId().equals(current.getId())
                && (!form.isEnabled() || !form.getRoles().contains(RoleType.ADMIN.name()))) {
            throw new IllegalStateException(
                    "You cannot disable your own account or remove your own ADMIN role.");
        }
        user.setEmail(form.getEmail().trim());
        user.setEnabled(form.isEnabled());
        user.setRoles(resolveRoles(form.getRoles()));

        employeeRepository.findByUserId(user.getId()).ifPresent(linked -> linked.setUser(null));
        linkEmployee(user, form.getEmployeeId());
        return userRepository.save(user);
    }

    @Transactional
    public void toggleEnabled(Long id, User current) {
        User user = findById(id);
        if (user.getId().equals(current.getId())) {
            throw new IllegalStateException("You cannot disable your own account.");
        }
        user.setEnabled(!user.isEnabled());
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        findById(id).setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * Verifies the current password before replacing it.
     * @throws IllegalArgumentException when the current password does not match
     */
    @Transactional
    public void changeOwnPassword(String currentPassword, String newPassword) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private void linkEmployee(User user, Long employeeId) {
        if (employeeId == null) {
            return;
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        if (employee.getUser() != null && !employee.getUser().getId().equals(user.getId())) {
            throw new DuplicateResourceException(
                    "Employee " + employee.getEmployeeCode() + " is already linked to another user");
        }
        employee.setUser(user);
    }

    private Set<Role> resolveRoles(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new DuplicateResourceException("At least one role must be assigned");
        }
        return roleNames.stream()
                .map(RoleType::valueOf)
                .map(type -> roleRepository.findByName(type)
                        .orElseThrow(() -> new IllegalStateException("Role not found in database: " + type)))
                .collect(Collectors.toSet());
    }
}
