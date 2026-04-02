package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

/**
 * Global exception handler — catches custom exceptions thrown from ANY controller/service
 * and returns a consistent JSON error response with the correct HTTP status code.
 *
 * HOW IT WORKS:
 * 1. Implementation throws:  throw new ResourceNotFoundException("Student not found")
 * 2. Exception bubbles up through Service → Controller → Spring
 * 3. Spring sees @RestControllerAdvice and checks if any @ExceptionHandler matches
 * 4. handleResourceNotFound() catches it → builds ErrorResponse → returns 404
 *
 * WITHOUT this class:
 *   - Custom RuntimeExceptions would return 500 Internal Server Error
 *   - You'd need ResponseStatusException everywhere (tightly coupled to HTTP)
 *
 * WITH this class:
 *   - Implementations throw clean domain exceptions (no HTTP knowledge needed)
 *   - This handler maps exceptions → HTTP status codes in ONE place
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Catches ResourceNotFoundException → returns 404 NOT FOUND
     *
     * Triggered by:
     *   - studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"))
     *   - teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Catches ResourceAlreadyExistsException → returns 409 CONFLICT
     *
     * Triggered by:
     *   - throw new ResourceAlreadyExistsException("Email already exists")
     *   - throw new ResourceAlreadyExistsException("Subject already exists")
     *   - throw new ResourceAlreadyExistsException("Teacher already has a subject assigned")
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Catches StudentExceptions → returns 400 BAD REQUEST
     * (Your existing custom exception class — kept for backward compatibility)
     */
    @ExceptionHandler(StudentExceptions.class)
    public ResponseEntity<ErrorResponse> handleStudentException(
            StudentExceptions ex, HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}

