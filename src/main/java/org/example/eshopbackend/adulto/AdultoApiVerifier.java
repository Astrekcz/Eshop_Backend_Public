// org/example/zeniqbackend/adulto/AdultoApiVerifier.java
package org.example.eshopbackend.adulto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.adulto.AdultoApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Profile({"sandbox","prod"})
@RequiredArgsConstructor
@Slf4j
public class AdultoApiVerifier implements AdultoVerifier {


    private final AdultoProperties props;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private HttpClient client() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
    }

    @Override
    public AdultoVerification verify(String adultoUid, int requiredAge) {
        if (adultoUid == null || adultoUid.isBlank()) {
            return new AdultoVerification(false, false, String.valueOf(requiredAge), "API", null);
        }

        try {
            // Base: např. https://api.result.adulto.cz
            String base = props.getResultUrl();
            if (base == null || base.isBlank()) {
                base = "https://api.result.adulto.cz";
            }
            // ořež koncová lomítka
            base = base.replaceAll("/+$", "");

            // Sestavení query ?secret=...&response=...
            String query = "secret=" + enc(props.getPrivateKey())
                    + "&response=" + enc(adultoUid);
            // Pokud by API někdy bralo i požadovaný věk:
            // query += "&requiredAge=" + requiredAge;

            // KLÍČOVÉ: použij /? + query (žádné přidávání / před parametry)
            URI uri = URI.create(base + "/?" + query);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .build();

            HttpResponse<String> resp = client().send(req, HttpResponse.BodyHandlers.ofString());

            if (props.isDebug()) {
                log.info("Adulto request URI: {}", uri);
                log.info("Adulto response ({}): {}", resp.statusCode(), resp.body());
            }

            boolean httpOk = resp.statusCode() >= 200 && resp.statusCode() < 300;
            if (!httpOk || resp.body() == null || resp.body().isBlank()) {
                return new AdultoVerification(false, false, String.valueOf(requiredAge), "API", adultoUid);
            }

            AdultoApiResponse api = MAPPER.readValue(resp.body(), AdultoApiResponse.class);

            boolean status = toBool(api.verifyStatus());
            boolean adult  = toBool(api.verifyAdult());
            String method  = api.verifyMethod() != null ? api.verifyMethod().toUpperCase() : "API";
            String reqAge  = api.verifyAge() != null ? api.verifyAge() : String.valueOf(requiredAge);

            return new AdultoVerification(status, adult, reqAge, method, api.verifyUid());

        } catch (Exception e) {
            log.error("Adulto verification failed", e);
            return new AdultoVerification(false, false, String.valueOf(requiredAge), "API", adultoUid);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static boolean toBool(String s) {
        return s != null && s.trim().equalsIgnoreCase("true");
    }
}
