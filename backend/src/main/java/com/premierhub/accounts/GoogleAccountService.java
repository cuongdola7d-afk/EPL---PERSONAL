package com.premierhub.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Validator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Instant;

@Service
public class GoogleAccountService {
    private final AccountRepository accounts;
    private final GoogleIdentityRepository identities;
    private final Validator validator;

    public GoogleAccountService(AccountRepository accounts, GoogleIdentityRepository identities, Validator validator) {
        this.accounts = accounts;
        this.identities = identities;
        this.validator = validator;
    }

    public record Profile(@NotBlank @Size(max = 255) String subject,
                          @NotBlank @Email @Size(max = 254) String email, String name) implements Serializable { }

    Profile verifiedProfile(OidcUser user) {
        // This is called only after Spring's OIDC authentication, signature/issuer/audience/nonce validation.
        if (!Boolean.TRUE.equals(user.getIdToken().getEmailVerified())) throw new GoogleAccountException("invalid_identity");
        var profile = new Profile(user.getIdToken().getSubject(), AccountService.normalizeEmail(user.getIdToken().getEmail()), user.getFullName());
        if (!validator.validate(profile).isEmpty()) throw new GoogleAccountException("invalid_identity");
        return profile;
    }

    @Transactional
    public AccountResponse login(Profile profile) {
        var existingId = identities.accountId(profile.subject());
        if (existingId.isPresent()) return account(existingId.get()).response();
        if (accounts.findByEmail(profile.email()).isPresent()) throw new GoogleAccountException("link_required");
        try {
            String name = profile.name() == null ? "" : profile.name().replaceAll("\\p{Cntrl}", "").strip();
            if (name.length() < 2) name = "Người chơi Google";
            if (name.length() > 80) name = name.substring(0, 80);
            var created = accounts.create(profile.email(), name, null, Instant.now());
            identities.link(created.id(), profile.subject());
            return created.response();
        } catch (DuplicateKeyException race) {
            // Roll back BOTH account and identity on a race; do not leave an orphan account or merge by email.
            throw new GoogleAccountException("try_again");
        }
    }

    @Transactional
    public void link(long accountId, Profile profile) {
        account(accountId);
        var existingId = identities.accountId(profile.subject());
        if (existingId.isPresent()) {
            if (existingId.get() != accountId) throw new GoogleAccountException("identity_linked");
            return; // Idempotent for the same account.
        }
        try { identities.link(accountId, profile.subject()); }
        catch (DuplicateKeyException race) { throw new GoogleAccountException("try_again"); }
    }

    Account account(long id) { return accounts.findById(id).orElseThrow(() -> new GoogleAccountException("session_changed")); }
    Account current(String email) { return accounts.findByEmail(email).orElseThrow(() -> new GoogleAccountException("session_changed")); }
    boolean linked(long accountId) { return identities.linked(accountId); }
}
