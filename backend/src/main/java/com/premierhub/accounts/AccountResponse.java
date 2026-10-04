package com.premierhub.accounts;

import java.time.Instant;

/** Private account response: never contains a password hash or session identifier. */
public record AccountResponse(long id, String email, String displayName, String role, Instant createdAt) { }
