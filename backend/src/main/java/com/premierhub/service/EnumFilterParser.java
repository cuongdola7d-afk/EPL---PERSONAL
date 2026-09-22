package com.premierhub.service;

import com.premierhub.web.error.InvalidFilterException;
import java.util.Locale;

final class EnumFilterParser {
    private EnumFilterParser() {
    }

    static <E extends Enum<E>> E parse(String value, Class<E> type, String filterName) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(filterName + " must not be blank");
        }
        try {
            return Enum.valueOf(type, normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidFilterException("Unknown " + filterName + ": " + normalized);
        }
    }
}
