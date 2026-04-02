package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

/**
 * Thrown when trying to create a resource that already exists (duplicate).
 * Caught by GlobalExceptionHandler → returns 409 CONFLICT.
 *
 * Usage:
 *   throw new ResourceAlreadyExistsException("Email already exists");
 *   throw new ResourceAlreadyExistsException("Subject already exists");
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}

