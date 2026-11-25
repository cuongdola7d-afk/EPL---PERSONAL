package com.premierhub.accounts;

/**
 * Reserved non-loginable system account used as the actor for automated Fantasy actions.
 */
public final class SystemAccount {
    public static final String EMAIL = "system@prismaxi.invalid";
    public static final String DISPLAY_NAME = "Hệ thống";

    private SystemAccount() {
    }

    public static boolean isSystemEmail(String email) {
        return email != null && EMAIL.equalsIgnoreCase(email.strip());
    }
}
