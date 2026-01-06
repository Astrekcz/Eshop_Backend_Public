package org.example.eshopbackend.util;

public final class VsUtil {
    private VsUtil() {}
    /** Např. "2025-00001" -> "202500001" (max 10 číslic) */
    public static String fromOrderNumber(String orderNumber) {
        String digits = orderNumber == null ? "" : orderNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) return "0";
        return digits.length() > 10 ? digits.substring(0, 10) : digits;
    }
}