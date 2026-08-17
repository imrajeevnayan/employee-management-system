package com.ems.web;

import com.ems.dto.PasswordChangeForm;
import com.ems.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String show(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        return "profile/show";
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("form", new PasswordChangeForm());
        return "profile/password";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("form") PasswordChangeForm form,
                                 BindingResult bindingResult, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            return "profile/password";
        }
        try {
            userService.changeOwnPassword(form.getCurrentPassword(), form.getNewPassword());
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("currentPassword", "wrong", ex.getMessage());
            return "profile/password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Your password has been changed");
        return "redirect:/profile";
    }
}
