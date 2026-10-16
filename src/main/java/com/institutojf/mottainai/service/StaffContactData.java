package com.institutojf.mottainai.service;

import java.util.Locale;

/** Normalizes readable staff contact data before persistence and lookup. */
final class StaffContactData {

    private StaffContactData() {
    }

    static String name(String value) {
        return value == null ? null : value.strip();
    }

    static String email(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }

    static String phone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
