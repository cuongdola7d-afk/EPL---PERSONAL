package com.premierhub.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidFilterException.class)
    public ResponseEntity<ApiErrorResponse> invalidFilter(
            InvalidFilterException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_FILTER", exception.getMessage(), request);
    }

    @ExceptionHandler({HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> validation(Exception exception,
                                                       HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> typeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
                "Invalid value for parameter: " + exception.getName(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> missingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Missing required parameter: " + exception.getParameterName(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception,
                                                        HttpServletRequest request) {
        LOG.error("Unexpected error while handling {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code,
                                                    String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
