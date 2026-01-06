package org.example.eshopbackend.util;

// src/main/java/.../util/Spayd.java
public final class Spayd {
    private Spayd() {}
    public static String build(String iban, Long amountCzk, String vs, String message, String bic) {
        if (iban == null || !iban.matches("^[A-Z0-9]{15,34}$"))
            throw new IllegalArgumentException("Neplatný IBAN");
        if (vs != null && !vs.matches("^\\d{1,10}$"))
            throw new IllegalArgumentException("VS musí být 1–10 číslic");
        StringBuilder sb = new StringBuilder("SPD*1.0*");
        sb.append("ACC:").append(iban);
        if (bic != null && !bic.isBlank()) sb.append("+").append(bic.toUpperCase());
        sb.append("*");
        if (amountCzk != null) {
            String am = String.format(java.util.Locale.ROOT, "%.2f", amountCzk / 100.0); // haléře -> Kč
            sb.append("AM:").append(am).append("*");
        }
        sb.append("CC:CZK*");
        if (vs != null) sb.append("X-VS:").append(vs).append("*");
        if (message != null && !message.isBlank())
            sb.append("MSG:").append(urlEncode(message)).append("*");
        return sb.substring(0, sb.length() - 1); // uřízni poslední *
    }
    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
