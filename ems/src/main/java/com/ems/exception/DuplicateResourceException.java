package com.ems.exception;

/** Thrown when a uniqueness or state constraint is violated (e.g. duplicate email). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
