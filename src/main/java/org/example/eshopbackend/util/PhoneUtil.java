package org.example.eshopbackend.util;
import com.google.i18n.phonenumbers.*;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class PhoneUtil {
    private static final PhoneNumberUtil PN = PhoneNumberUtil.getInstance();

    /** Vrátí E.164 (+420...) nebo null. defaultRegion např. "CZ". */
    public static String toE164OrNull(String raw, String defaultRegion) {
        if (raw == null || raw.isBlank()) return null;
        try {
            PhoneNumber num = PN.parse(raw.trim(), defaultRegion);
            if (!PN.isValidNumber(num)) return null;
            return PN.format(num, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return null;
        }
    }
}