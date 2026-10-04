package com.premierhub.accounts;

class GoogleAccountException extends RuntimeException {
    private final String code;
    GoogleAccountException(String code) { super("Google account operation failed"); this.code = code; }
    String code() { return code; }
}
