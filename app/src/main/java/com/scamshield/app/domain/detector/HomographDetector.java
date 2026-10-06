package com.scamshield.app.domain.detector;

import java.net.IDN;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HomographDetector {

    // Common Cyrillic and Greek lookalike characters targeting Latin letters
    private static final Map<Character, Character> LOOKALIKE_MAP = new HashMap<>();

    static {
        // Cyrillic lookalikes
        LOOKALIKE_MAP.put('\u0430', 'a'); // Cyrillic small letter a
        LOOKALIKE_MAP.put('\u0441', 'c'); // Cyrillic small letter es
        LOOKALIKE_MAP.put('\u0435', 'e'); // Cyrillic small letter ie
        LOOKALIKE_MAP.put('\u0456', 'i'); // Cyrillic small letter byelorussian-ukrainian i
        LOOKALIKE_MAP.put('\u0458', 'j'); // Cyrillic small letter je
        LOOKALIKE_MAP.put('\u043E', 'o'); // Cyrillic small letter o
        LOOKALIKE_MAP.put('\u0440', 'p'); // Cyrillic small letter er
        LOOKALIKE_MAP.put('\u0455', 's'); // Cyrillic small letter dze
        LOOKALIKE_MAP.put('\u0445', 'x'); // Cyrillic small letter ha
        LOOKALIKE_MAP.put('\u0443', 'y'); // Cyrillic small letter u

        // Greek lookalikes
        LOOKALIKE_MAP.put('\u03BF', 'o'); // Greek small letter omicron
        LOOKALIKE_MAP.put('\u03BD', 'v'); // Greek small letter nu
    }

    private static final Pattern PUNYCODE_PATTERN = Pattern.compile("(?i)^xn--|\\.xn--");

    /**
     * Checks if a domain uses Punycode (IDN) or contains mixed-script lookalike homoglyphs.
     */
    public static boolean containsHomographOrPunycode(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        // 1. Direct punycode prefix check
        if (PUNYCODE_PATTERN.matcher(host).find()) {
            return true;
        }

        // 2. IDN conversion check
        try {
            String ascii = IDN.toASCII(host);
            if (PUNYCODE_PATTERN.matcher(ascii).find()) {
                return true;
            }
        } catch (Exception ignored) {}

        // 3. Inspect characters for Cyrillic / Greek spoofing in domain
        boolean hasLatin = false;
        boolean hasLookalike = false;

        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLatin = true;
            } else if (LOOKALIKE_MAP.containsKey(c)) {
                hasLookalike = true;
            }
        }

        return hasLatin && hasLookalike;
    }

    /**
     * Normalizes a potential homoglyph spoofed string to Latin equivalent.
     */
    public static String normalizeHomoglyphs(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (LOOKALIKE_MAP.containsKey(c)) {
                sb.append(LOOKALIKE_MAP.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
