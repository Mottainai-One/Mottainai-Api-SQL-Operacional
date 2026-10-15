package com.institutojf.mottainai.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Produces the deterministic tokens required by the protected operational schema. */
public final class ProtectedDataToken {

    private ProtectedDataToken() {
    }

    public static String customerName(String value) {
        return token("CUS", normalized(value));
    }

    public static String cpf(String value) {
        return token("CPF", value == null ? null : value.replaceAll("\\D", ""));
    }

    public static String email(String value) {
        return token("EMAIL", value == null ? null : value.trim().toLowerCase(Locale.ROOT));
    }

    public static String phone(String value) {
        return token("PHONE", value == null ? null : value.replaceAll("\\D", ""));
    }

    public static String externalAuthUid(String value) {
        return token("AUTH", normalized(value));
    }

    public static String address(String zipCode, String street, String number, String complement,
            String neighborhood, String city, String state) {
        StringBuilder value = new StringBuilder();
        for (String part : new String[] { zipCode, street, number, complement, neighborhood, city, state }) {
            if (part == null) {
                continue;
            }
            if (!value.isEmpty()) {
                value.append('|');
            }
            value.append(part.trim());
        }
        return token("ADDR", value.toString());
    }

    private static String normalized(String value) {
        return value == null ? null : value.trim();
    }

    private static String token(String prefix, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.matches("^" + prefix + "_SHA256_[A-Fa-f0-9]{64}$")) {
            return value.toUpperCase(Locale.ROOT);
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(64);
            for (byte item : hash) {
                hexadecimal.append(String.format("%02X", item));
            }
            return prefix + "_SHA256_" + hexadecimal;
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
