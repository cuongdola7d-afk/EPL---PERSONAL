package com.premierhub.accounts;

import java.time.Instant;

public record Account(long id, String email, String displayName, String passwordHash, String role, Instant createdAt) {
    AccountResponse response() { return new AccountResponse(id, email, displayName, role, createdAt); }

    @Override public String toString() { return "Account[id=" + id + "]"; }
}
