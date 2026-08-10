package com.example.minitask_pacto_mais.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public static List<String> lookupCandidates(String raw, String defaultCountry) {
        String primary = normalize(raw, defaultCountry);
        if (primary == null) {
            return List.of();
        }

        Set<String> out = new LinkedHashSet<>();
        out.add(primary);

        String digits = primary.startsWith("+") ? primary.substring(1) : primary;
        if (digits.startsWith("55") && digits.length() == 12) {
            out.add("+" + digits.substring(0, 4) + "9" + digits.substring(4));
        }
        if (digits.startsWith("55") && digits.length() == 13 && digits.charAt(4) == '9') {
            out.add("+" + digits.substring(0, 4) + digits.substring(5));
        }

        return new ArrayList<>(out);
    }
}
