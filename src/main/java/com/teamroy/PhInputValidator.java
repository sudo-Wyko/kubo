package com.teamroy;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PhInputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PH_MOBILE = Pattern.compile("^(09|\\+639)\\d{9}$");

    private PhInputValidator() {
    }

    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\s-]", "");
    }

    public static Optional<String> validateContactRequired(String contact) {
        String normalized = normalizePhone(contact);
        if (normalized.isBlank()) {
            return Optional.of("Contact number is required.");
        }
        if (!PH_MOBILE.matcher(normalized).matches()) {
            return Optional.of("Enter a valid PH mobile number (09XXXXXXXXX or +639XXXXXXXXX).");
        }
        return Optional.empty();
    }

    public static Optional<String> validateEmailOptional(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        if (!EMAIL.matcher(email.trim()).matches()) {
            return Optional.of("Enter a valid email address.");
        }
        return Optional.empty();
    }
}
