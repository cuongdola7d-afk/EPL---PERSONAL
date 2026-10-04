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
@RestControllerAdvice(assignableTypes = {AuthController.class, GoogleAuthController.class})
public class AuthExceptionHandler {
    @ExceptionHandler(GoogleAccountException.class)
    ResponseEntity<?> google(GoogleAccountException exception) {
        String code = exception.code();
        int status = "unavailable".equals(code) ? 503 : "login_required".equals(code) ? 401 : 409;
        String message = switch (code) {
            case "unavailable" -> "Đăng nhập Google chưa được cấu hình. Bạn vẫn có thể dùng email và mật khẩu.";
            case "login_required" -> "Đăng nhập tài khoản hiện có trước khi liên kết Google.";
            case "identity_linked" -> "Google này đã được liên kết với tài khoản khác.";
            case "already_signed_in" -> "Bạn đã đăng nhập. Hãy dùng thao tác liên kết Google trong tài khoản.";
            case "link_required" -> "Email này đã có tài khoản. Hãy đăng nhập tài khoản đó rồi xác nhận liên kết Google.";
            default -> "Phiên liên kết không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.";
        };
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
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
