package com.ems.exception;

import com.ems.domain.User;
import com.ems.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-in user to every view (topbar, profile menus).
 * Null when the request is anonymous (login page, error pages).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final UserService userService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        return userService.getCurrentUserOrNull();
    }
}
