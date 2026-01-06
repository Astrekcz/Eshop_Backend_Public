package org.example.eshopbackend.shipping.ppl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.config.PplProps;
import org.example.eshopbackend.shipping.ppl.auth.PplTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PplWebClientFactory {
    private final PplProps props;
    private final PplTokenService tokenService;

    @PostConstruct
    void logConfig() {
        log.info("[PPL] apiBase={}", props.getApiBase());
        log.info("[PPL] tokenUrl={}", props.getOauth().getTokenUrl());
    }

    @Bean("pplWebClient")
    public WebClient pplWebClient() {
        final String baseUrl = Objects.requireNonNull(
                props.getApiBase(), "ppl.api-base missing! Add ppl.api-base to application.yml");
        if (!baseUrl.startsWith("http")) {
            throw new IllegalStateException("ppl.api-base must start with http/https: " + baseUrl);
        }

        ExchangeFilterFunction authFilter = (request, next) -> {
            String token = tokenService.getAccessToken();
            ClientRequest authorized = ClientRequest.from(request)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            return next.exchange(authorized).flatMap(response -> {
                int sc = response.statusCode().value();
                String wwwAuth = response.headers().asHttpHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
                boolean invalidToken = wwwAuth != null && wwwAuth.toLowerCase().contains("invalid_token");

                if (sc == 401 || sc == 403 || invalidToken) {
                    log.warn("[PPL] {} detected (invalid_token? {}), forcing token refresh and retrying once.", sc, invalidToken);
                    String fresh = tokenService.forceRefresh();
                    ClientRequest retry = ClientRequest.from(request)
                            .headers(h -> {
                                h.remove(HttpHeaders.AUTHORIZATION);
                                h.add(HttpHeaders.AUTHORIZATION, "Bearer " + fresh);
                            }).build();
                    return next.exchange(retry);
                }
                return Mono.just(response);
            });
        };

        // Neplaš logy pro očekávané 404 na /shipment/batch/{id}/label
        ExchangeFilterFunction smartLogger = (request, next) ->
                next.exchange(request).flatMap(resp -> {
                    if (resp.statusCode().is4xxClientError() || resp.statusCode().is5xxServerError()) {
                        final String path = request.url().getPath();
                        final int sc = resp.statusCode().value();
                        if (sc == 404 && path != null && path.contains("/shipment/batch/") && path.endsWith("/label")) {
                            if (log.isDebugEnabled()) {
                                log.debug("[PPL] label not ready yet (404) for {}", path);
                            }
                            return Mono.just(resp); // žádné ERROR logy
                        }
                        // stručně zaloguj – body nenačítáme, aby se nespotřebovalo
                        log.error("[PPL] {} {} -> {}", request.method(), request.url(), resp.statusCode());
                    }
                    return Mono.just(resp);
                });

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .compress(true);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "cs")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(authFilter)
                .filter(smartLogger)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();
    }
}
