package com.premierhub.accounts;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
public class AccountService implements UserDetailsService {
    private final AccountRepository repository;
    private final PasswordEncoder passwords;

    public AccountService(AccountRepository repository, PasswordEncoder passwords) {
        this.repository = repository;
        this.passwords = passwords;
    }

    static String normalizeEmail(String email) { return email == null ? null : email.strip().toLowerCase(Locale.ROOT); }

    public AccountResponse register(String email, String displayName, String password) {
        validatePasswordBytes(password);
        String normalized = normalizeEmail(email);
        if (repository.findByEmail(normalized).isPresent()) throw new DuplicateEmailException();
        try {
            return repository.create(normalized, displayName.strip(), passwords.encode(password), Instant.now()).response();
        } catch (DuplicateKeyException duplicate) {
            // The SQL unique constraint also protects simultaneous registrations.
            throw new DuplicateEmailException();
        }
    }

    static void validatePasswordBytes(String password) {
        // BCrypt must not silently truncate a multi-byte password.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AccountInputException("Mật khẩu không được vượt quá 72 byte UTF-8.");
        }
    }

    public AccountResponse current(String email) {
        return repository.findByEmail(normalizeEmail(email)).orElseThrow(() -> new UsernameNotFoundException("Account unavailable")).response();
    }

    @Override public UserDetails loadUserByUsername(String email) {
        var account = repository.findByEmail(normalizeEmail(email)).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        if (account.passwordHash() == null) throw new UsernameNotFoundException("Password login unavailable");
        return User.withUsername(account.email()).password(account.passwordHash()).roles(account.role()).build();
    }
}
