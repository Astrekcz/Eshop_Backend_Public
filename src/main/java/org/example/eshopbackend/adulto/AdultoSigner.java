package org.example.eshopbackend.adulto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

public class AdultoSigner {

    public record Signature(String timestamp, String signature) {}

    /**
     * Vzorově: HMAC-SHA256 z (timestamp + "\n" + body), klíč = privateKey
     */
    public static Signature hmacSha256(String privateKey, String body) {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String payload = ts + "\n" + (body == null ? "" : body);

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(raw);
            return new Signature(ts, hex);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }
}
