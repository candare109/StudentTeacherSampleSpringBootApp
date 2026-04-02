package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

/**
 * Thrown when a requested resource (Student, Teacher, Subject) is not found in the database.
 * Caught by GlobalExceptionHandler → returns 404 NOT FOUND.
 *
 * Usage in Implementation:
 *   throw new ResourceNotFoundException("Student not found");
 *   throw new ResourceNotFoundException("Teacher not found with id: " + id);
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

