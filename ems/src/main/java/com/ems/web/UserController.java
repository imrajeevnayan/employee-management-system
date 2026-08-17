package com.ems.web;

import com.ems.domain.RoleType;
import com.ems.domain.User;
import com.ems.dto.PasswordPolicy;
import com.ems.dto.ResetPasswordForm;
import com.ems.dto.UserForm;
import com.ems.exception.DuplicateResourceException;
import com.ems.service.EmployeeService;
import com.ems.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/** Admin-only user & role management; URL rule /users/** already restricts to ADMIN. */
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmployeeService employeeService;

    @ModelAttribute("allRoles")
    public List<String> allRoles() {
        return Arrays.stream(RoleType.values()).map(Enum::name).toList();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.listAll());
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        UserForm form = new UserForm();
        model.addAttribute("form", form);
        model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
        return "users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (!PasswordPolicy.isValid(form.getPassword())) {
            bindingResult.rejectValue("password", "policy", PasswordPolicy.MESSAGE);
        }
        if (form.getRoles().isEmpty()) {
            bindingResult.rejectValue("roles", "required", "Assign at least one role");
        }
        if (userService.usernameTaken(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "This username is already taken");
        }
        if (userService.emailTaken(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "This email is already in use");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
            return "users/form";
        }
        try {
            userService.create(form);
        } catch (DuplicateResourceException ex) {
            bindingResult.reject("duplicate", ex.getMessage());
            model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
            return "users/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "User '" + form.getUsername() + "' created successfully");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setEnabled(user.isEnabled());
        user.getRoles().forEach(role -> form.getRoles().add(role.getName().name()));
        if (user.getEmployee() != null) {
            form.setEmployeeId(user.getEmployee().getId());
        }
        model.addAttribute("form", form);
        model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (form.getRoles().isEmpty()) {
            bindingResult.rejectValue("roles", "required", "Assign at least one role");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
            return "users/form";
        }
        try {
            userService.update(id, form, userService.getCurrentUser());
        } catch (DuplicateResourceException ex) {
            bindingResult.reject("duplicate", ex.getMessage());
            model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
            return "users/form";
        } catch (IllegalStateException ex) {
            bindingResult.reject("selflock", ex.getMessage());
            model.addAttribute("unlinkedEmployees", employeeService.unlinkedEmployees());
            return "users/form";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "User '" + form.getUsername() + "' updated successfully");
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        try {
            userService.toggleEnabled(id, userService.getCurrentUser());
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + user.getUsername() + (user.isEnabled() ? "' enabled" : "' disabled"));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/users";
    }

    @GetMapping("/{id}/password")
    public String resetPasswordForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("form", new ResetPasswordForm());
        return "users/password";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable Long id,
                                @Valid @ModelAttribute("form") ResetPasswordForm form,
                                BindingResult bindingResult, Model model,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        if (!bindingResult.hasErrors() && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "users/password";
        }
        userService.resetPassword(id, form.getPassword());
        redirectAttributes.addFlashAttribute("successMessage",
                "Password reset for '" + user.getUsername() + "'");
        return "redirect:/users";
    }
}
