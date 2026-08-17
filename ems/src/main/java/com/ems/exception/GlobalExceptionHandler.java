package com.ems.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Maps domain exceptions to friendly error pages. Business-rule violations that
 * occur inside form flows (duplicate email, delete with children, self-lockout)
 * are handled locally in controllers so the user's input is preserved; this
 * advice is the safety net for anything that escapes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(ResourceNotFoundException ex) {
        return errorView("404", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView accessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return errorView("403", ex.getMessage());
    }

    @ExceptionHandler({DuplicateResourceException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView conflict(RuntimeException ex) {
        return errorView("400", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return errorView("500", "An unexpected error occurred. The issue has been logged.");
    }

    private ModelAndView errorView(String code, String message) {
        ModelAndView modelAndView = new ModelAndView("error/" + code);
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
