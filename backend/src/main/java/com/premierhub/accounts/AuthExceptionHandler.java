package com.premierhub.accounts;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("code", "INVALID_INPUT", "message", "Thông tin gửi lên không hợp lệ.")); }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<?> duplicate() { return ResponseEntity.status(409).body(Map.of("code", "EMAIL_EXISTS", "message", "Email này đã được đăng ký.")); }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<?> credentials() { return ResponseEntity.status(401).body(Map.of("code", "INVALID_CREDENTIALS", "message", "Email hoặc mật khẩu không đúng.")); }

    @ExceptionHandler(AccountInputException.class)
    ResponseEntity<?> input(AccountInputException exception) { return ResponseEntity.badRequest().body(Map.of("code", "INVALID_INPUT", "message", exception.getMessage())); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
        // Only field names are used; never include rejected passwords or request bodies.
        String field = exception.getBindingResult().getFieldErrors().stream().map(error -> error.getField()).findFirst().orElse("");
        String message = switch (field) {
            case "email" -> "Nhập địa chỉ email hợp lệ, tối đa 254 ký tự.";
            case "displayName" -> "Tên hiển thị phải có từ 2 đến 80 ký tự, không chứa ký tự điều khiển.";
            case "password" -> "Mật khẩu phải có từ 8 đến 72 ký tự khi đăng ký, tối đa 72 byte UTF-8.";
            default -> "Kiểm tra lại thông tin đã nhập.";
        };
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_INPUT", "message", message, "field", field));
    }
}
