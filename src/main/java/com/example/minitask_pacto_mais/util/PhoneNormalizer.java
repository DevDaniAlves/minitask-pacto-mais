package com.example.minitask_pacto_mais.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String raw, String defaultCountry) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (raw.trim().startsWith("+")) {
            return "+" + digits;
        }

        if (defaultCountry != null
                && !defaultCountry.isBlank()
                && digits.startsWith(defaultCountry)
                && digits.length() > defaultCountry.length() + 8) {
            return "+" + digits;
        }

        if (defaultCountry != null && !defaultCountry.isBlank()) {
            return "+" + defaultCountry + digits;
        }

        return "+" + digits;
    }
}
