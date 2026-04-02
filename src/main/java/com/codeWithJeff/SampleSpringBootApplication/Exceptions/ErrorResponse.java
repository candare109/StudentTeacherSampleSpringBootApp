package com.codeWithJeff.SampleSpringBootApplication.Exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Standard error response returned by GlobalExceptionHandler.
 * Gives the client a consistent JSON error structure:
 *
 * {
 *   "timestamp": "2026-04-01T12:00:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Student not found",
 *   "path": "/api/students/999"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}

