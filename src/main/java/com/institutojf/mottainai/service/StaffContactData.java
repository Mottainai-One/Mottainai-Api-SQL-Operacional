package com.institutojf.mottainai.service;

import com.institutojf.mottainai.exception.BusinessException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalizes readable staff contact data before persistence and lookup. */
final class StaffContactData {

    private static final Pattern CPF_DIGITS = Pattern.compile("\\d{11}");

    private static final Pattern CPF_FORMATTED = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

    private StaffContactData() {
    }

    static String name(String value) {
        return emptyToNull(value == null ? null : value.strip());
    }

    static String email(String value) {
        String normalized = value == null ? null : value.strip().toLowerCase(Locale.ROOT);
        return emptyToNull(normalized);
    }

    static String phone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    static String cpf(String value) {
        String input = value == null ? null : value.strip();
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (!CPF_DIGITS.matcher(input).matches() && !CPF_FORMATTED.matcher(input).matches()) {
            throw new BusinessException("CPF must contain 11 digits");
        }
        String normalized = input.replaceAll("\\D", "");
        if (!hasValidCpfChecksum(normalized)) {
            throw new BusinessException("CPF is invalid");
        }
        return normalized;
    }

    static boolean hasValidCpfChecksum(String cpf) {
        if (cpf == null || !CPF_DIGITS.matcher(cpf).matches()
                || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int firstDigit = calculateCpfDigit(cpf, 9, 10);
        int secondDigit = calculateCpfDigit(cpf, 10, 11);
        return cpf.charAt(9) - '0' == firstDigit && cpf.charAt(10) - '0' == secondDigit;
    }

    private static int calculateCpfDigit(String cpf, int length, int firstWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (cpf.charAt(index) - '0') * (firstWeight - index);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
