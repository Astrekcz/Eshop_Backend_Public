package org.example.eshopbackend.shipping.ppl.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.config.PplProps;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PplTokenService {
    private final PplProps props;
    private final WebClient webClient = WebClient.builder().build();
    private final AtomicReference<TokenCache> cacheRef = new AtomicReference<>();

    public synchronized String getAccessToken() {
        var cache = cacheRef.get();
        if (cache != null && cache.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
            return cache.token;
        }
        return fetchNewToken();
    }

    public synchronized String forceRefresh() {
        cacheRef.set(null);
        return fetchNewToken();
    }

    public synchronized void invalidate() {
        cacheRef.set(null);
    }

    private String fetchNewToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getOauth().getClientId());
        form.add("client_secret", props.getOauth().getClientSecret());
        if (props.getOauth().getScope() != null && !props.getOauth().getScope().isBlank()) {
            form.add("scope", props.getOauth().getScope());
        }

        var resp = webClient.post()
                .uri(props.getOauth().getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (resp == null || resp.access_token == null) {
            throw new IllegalStateException("PPL OAuth: access_token je null");
        }
        long expires = resp.expires_in != null ? resp.expires_in : 300L;
        var expiresAt = Instant.now().plusSeconds(Math.max(60, expires - 30));
        cacheRef.set(new TokenCache(resp.access_token, expiresAt));
        log.info("PPL OAuth: získán token, exp za ~{}s", expires);
        return resp.access_token;
    }

    private record TokenResponse(String access_token, String token_type, Long expires_in) {}
    private record TokenCache(String token, Instant expiresAt) {}
}
