package org.example.eshopbackend.shipping.ppl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.config.PplProps;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentRequestDTO;
import org.example.eshopbackend.dto.shipment.PplCreateShipmentResultDTO;
import org.example.eshopbackend.dto.shipment.PplTrackingStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PplClientImpl implements PplClient {

    @Qualifier("pplWebClient")
    private final WebClient pplWebClient;
    private final PplProps props;

    @Override
    public PplCreateShipmentResultDTO createShipmentWithLabel(
            PplCreateShipmentRequestDTO req,
            int pieces,
            String format,
            Integer dpi,
            boolean completeLabelRequested,
            String depot
    ) {
        // 1) POST /shipment/batch
        BatchRequest payload = buildBatchRequest(req, pieces, format, dpi, completeLabelRequested, depot);

        ResponseEntity<BatchCreateResponse> createResp = pplWebClient.post()
                .uri("/shipment/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[PPL] POST /shipment/batch -> {}. Body: {}", resp.statusCode().value(), body);
                                    return Mono.error(new RuntimeException("PPL /shipment/batch failed: " + resp.statusCode().value() + " " + body));
                                })
                )
                .toEntity(BatchCreateResponse.class)
                .block();

        if (createResp == null) {
            throw new IllegalStateException("PPL /shipment/batch: prázdná odpověď");
        }

        String batchId = extractBatchIdFromLocation(createResp.getHeaders().getLocation());
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalStateException("PPL /shipment/batch: chybí Location nebo batchId");
        }

        // 2) Polling labelu – 404 = not ready, neházíme
        final int maxAttempts = 15;
        final long initialDelayMs = 300L;
        final double backoff = 1.5;
        final long maxDelayMs = 2000L;

        byte[] labelBytes = null;
        String labelMime = guessMimeFromFormat(format);

        long delay = initialDelayMs;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                String redirectUrl = pplWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/shipment/batch/{batchId}/label")
                                .queryParam("pageSize", "A4")
                                .queryParam("position", 1)
                                .queryParam("limit", 200)
                                .queryParam("offset", 0)
                                .build(batchId))
                        .exchangeToMono(resp -> {
                            if (resp.statusCode().is2xxSuccessful() || resp.statusCode().is3xxRedirection()) {
                                var loc = resp.headers().asHttpHeaders().getLocation();
                                return Mono.justOrEmpty(loc).map(Object::toString);
                            }
                            if (resp.statusCode().value() == 404) {
                                return Mono.empty();
                            }
                            return resp.createException().flatMap(Mono::error);
                        })
                        .block();

                ResponseEntity<byte[]> labelResp;
                if (redirectUrl != null && !redirectUrl.isBlank()) {
                    labelResp = pplWebClient.get()
                            .uri(redirectUrl)
                            .retrieve()
                            .toEntity(byte[].class)
                            .block();
                } else {
                    labelResp = pplWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/shipment/batch/{batchId}/label")
                                    .queryParam("pageSize", "A4")
                                    .queryParam("position", 1)
                                    .queryParam("limit", 200)
                                    .queryParam("offset", 0)
                                    .build(batchId))
                            .retrieve()
                            .toEntity(byte[].class)
                            .block();
                }

                if (labelResp != null && labelResp.getStatusCode().is2xxSuccessful() && labelResp.getBody() != null) {
                    var ct = labelResp.getHeaders().getContentType();
                    if (ct != null) labelMime = ct.toString();
                    labelBytes = labelResp.getBody();
                    log.info("[PPL] Label ready for batch {} (attempt {}).", batchId, (i + 1));
                    break;
                }

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound nf) {
                // 404 – čekáme dál
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                if (!e.getStatusCode().is5xxServerError()) throw e;
                // 5xx – retry
            }

            try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            delay = Math.min((long) (delay * backoff), maxDelayMs);
        }

        return PplCreateShipmentResultDTO.builder()
                .shipmentId(batchId)
                .batchId(batchId)
                .trackingNumber(null)
                .labelPdf(labelBytes)      // může být null
                .labelMime(labelMime)      // např. "application/pdf"
                .build();
    }

    @Override
    public PplTrackingStatus getStatus(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number is empty");
        }

        JsonNode root = pplWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/shipment")
                        .queryParam("ShipmentNumbers", trackingNumber)
                        .queryParam("Limit", 1)
                        .queryParam("Offset", 0)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) {
            throw new IllegalStateException("PPL /shipment: prázdná odpověď pro " + trackingNumber);
        }

        JsonNode item = firstItem(root);
        if (item == null) {
            return PplTrackingStatus.builder()
                    .trackingNumber(trackingNumber)
                    .rawStatus(null)
                    .description("Zásilka nenalezena")
                    .lastEventTime(null)
                    .lastHub(null)
                    .build();
        }

        String tn = textOrNull(item.path("shipmentNumber"), item.path("ShipmentNumber"));
        if (tn == null || tn.isBlank()) tn = trackingNumber;

        JsonNode tt = item.path("trackAndTrace");

        String raw = textOrNull(
                tt.path("lastEventCode"),
                item.path("status"),
                item.path("shipmentStatus"),
                item.path("statusCode"),
                item.path("state")
        );

        String desc = textOrNull(
                tt.path("lastEventName"),
                item.path("statusText"),
                item.path("statusDescription"),
                item.path("description"),
                item.path("statusName")
        );

        Instant lastTs = parseInstant(textOrNull(
                tt.path("lastEventDate"),
                item.path("lastUpdateDate"),
                item.path("lastEventTime"),
                item.path("statusDate")
        ));

        String hub = textOrNull(
                item.path("deliveryFeature").path("hub"),
                item.path("deliveryFeature").path("hubDate"),
                item.path("actualLocation"),
                item.path("lastHub"),
                item.path("branch"),
                item.path("location")
        );

        return PplTrackingStatus.builder()
                .trackingNumber(tn)
                .rawStatus(raw)
                .description(desc)
                .lastEventTime(lastTs)
                .lastHub(hub)
                .build();
    }

    // ------- helpers -------

    private BatchRequest buildBatchRequest(PplCreateShipmentRequestDTO req,
                                           int pieces,
                                           String format,
                                           Integer dpi,
                                           boolean completeLabelRequested,
                                           String depot) {

        var cls = new BatchRequest.LabelSettings.CompleteLabelSettings(
                completeLabelRequested, "A4", 1
        );
        var ls = new BatchRequest.LabelSettings(format, dpi, cls);

        var rc = new BatchRequest.ReturnChannel("Email", req.getRecipientEmail());

        // Absender z configu
        var sp = props.getSender();
        var sender = new BatchRequest.Address(
                sp.getName(), sp.getStreet(), sp.getCity(), sp.getZipCode(), sp.getCountry(),
                sp.getContact(), sp.getPhone(), sp.getEmail()
        );

        // Adresát z objednávky
        var recipient = new BatchRequest.Address(
                req.getRecipientName(), req.getRecipientStreet(), req.getRecipientCity(),
                req.getRecipientZip(), req.getRecipientCountry(),
                null, req.getRecipientPhone(), req.getRecipientEmail()
        );

        // --- hmotnost: vždy alespoň 0.01 kg ---
        int grams = req.getWeightGrams() == null ? 0 : Math.max(0, req.getWeightGrams());
        if (grams <= 0) {
            log.warn("[PPL] weightGrams nebylo dodáno, posílám 0.01 kg jako fallback.");
            grams = 10;
        }
        BigDecimal totalKg = BigDecimal.valueOf(grams).movePointLeft(3); // g -> kg

        int pc = Math.max(1, pieces);
        BigDecimal perPiece = weightPerPiece(totalKg, pc); // min 0.01, scale 2

        // vyrob shipmentSetItems s weighedShipmentInfo.weight
        List<BatchRequest.ShipmentSetItem> items = new ArrayList<>(pc);
        for (int i = 0; i < pc; i++) {
            items.add(new BatchRequest.ShipmentSetItem(
                    null, // shipmentNumber
                    new BatchRequest.WeighedShipmentInfo(perPiece.doubleValue()),
                    null, null, null
            ));
        }
        var set = new BatchRequest.ShipmentSet(pc, false, items);

        // AgeCheck kód (volitelně)
        String ageCheck = null;
        if (props.getAgeCheck() != null && props.getAgeCheck().isEnabled()) {
            String raw = props.getAgeCheck().getCode();
            if (raw != null && !raw.isBlank()) {
                String t = raw.trim().toUpperCase();
                ageCheck = t.matches("\\d+") ? ("A" + t) : t; // "18" -> "A18"
            } else {
                ageCheck = "A18";
            }
        }

        if (log.isInfoEnabled()) {
            log.info("[PPL] Building payload: order={} pieces={} totalWeightKg={} itemWeightKg={} ageCheck={}",
                    req.getOrderNumber(), pc, totalKg.setScale(2, RoundingMode.UP),
                    perPiece, ageCheck);
        }

        // DŮLEŽITÉ: neposílat prázdné objekty senderMask/directInjection → dát je skutečně null
        var shipment = new BatchRequest.Shipment(
                req.getServiceCode(),               // productType
                req.getOrderNumber(),               // referenceId
                null,                               // shipmentNumber
                null,                               // note
                depot != null ? depot : sp.getDepot(),
                sp.getIntegratorId(),
                set,
                null,                               // senderMask (NEPOSÍLÁME)
                sender,
                recipient,
                null,                               // specificDelivery
                null,                               // COD
                null,                               // insurance
                null,                               // externalNumbers
                null,                               // dormant
                null,                               // services (agecheck neposíláme jako service)
                ageCheck,                           // ageCheck pole
                null,                               // shipmentRouting
                null,                               // directInjection (NEPOSÍLÁME)
                null                                // labelService
        );

        return new BatchRequest(rc, ls, List.of(shipment), null);
    }

    private static BigDecimal weightPerPiece(BigDecimal totalKg, int pieces) {
        if (totalKg == null || totalKg.signum() <= 0) return new BigDecimal("0.01");
        BigDecimal pc = BigDecimal.valueOf(pieces);
        BigDecimal per = totalKg.divide(pc, 3, RoundingMode.UP); // mezivýpočet
        if (per.compareTo(new BigDecimal("0.01")) < 0) per = new BigDecimal("0.01");
        return per.setScale(2, RoundingMode.UP);
    }

    private String extractBatchIdFromLocation(URI location) {
        if (location == null) return null;
        String path = location.getPath();
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : null;
    }

    private String guessMimeFromFormat(String format) {
        if (format == null) return "application/pdf";
        String f = format.trim().toLowerCase();
        return f.equals("png") ? "image/png" : "application/pdf";
    }

    private static JsonNode firstItem(JsonNode root) {
        if (root == null || root.isNull()) return null;
        if (root.isArray()) return root.size() > 0 ? root.get(0) : null;

        JsonNode items = root.path("items");
        if (items.isArray() && items.size() > 0) return items.get(0);

        JsonNode shipments = root.path("shipments");
        if (shipments.isArray() && shipments.size() > 0) return shipments.get(0);

        JsonNode data = root.path("data");
        if (data.isArray() && data.size() > 0) return data.get(0);

        return null;
    }

    private static String textOrNull(JsonNode... nodes) {
        for (var n : nodes) {
            if (n != null && !n.isMissingNode() && !n.isNull()) {
                String v = n.asText(null);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            try {
                return java.time.LocalDateTime.parse(iso).toInstant(java.time.ZoneOffset.UTC);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // ------- wire typy pro batch flow (jen co fakt potřebujeme) -------

    private static class BatchCreateResponse {
        // tělo nás nezajímá; čteme Location header
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BatchRequest(
            ReturnChannel returnChannel,
            LabelSettings labelSettings,
            List<Shipment> shipments,
            String shipmentsOrderBy
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record ReturnChannel(String type, String address) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record LabelSettings(String format, Integer dpi, CompleteLabelSettings completeLabelSettings) {
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private record CompleteLabelSettings(boolean isCompleteLabelRequested, String pageSize, Integer position) {}
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record Shipment(
                String productType,
                String referenceId,
                String shipmentNumber,
                String note,
                String depot,
                String integratorId,
                ShipmentSet shipmentSet,
                AddressMask senderMask,
                Address sender,
                Address recipient,
                SpecificDelivery specificDelivery,
                CashOnDelivery cashOnDelivery,
                Insurance insurance,
                List<ExternalNumber> externalNumbers,
                Dormant dormant,
                List<Service> services,
                String ageCheck,
                ShipmentRouting shipmentRouting,
                DirectInjection directInjection,
                LabelService labelService
        ) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record ShipmentSet(Integer numberOfShipments, Boolean additionallyAdded, List<ShipmentSetItem> shipmentSetItems) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record ShipmentSetItem(String shipmentNumber, WeighedShipmentInfo weighedShipmentInfo,
                                       List<ExternalNumber> externalNumbers, Insurance insurance, Object _unused) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record WeighedShipmentInfo(Double weight) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record AddressMask(String country, String zipCode, String name, String name2, String street, String city, String contact, String phone) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record Address(String name, String street, String city, String zipCode, String country,
                               String contact, String phone, String email) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record SpecificDelivery(String parcelShopCode) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record CashOnDelivery(String codCurrency, Double codPrice, String codVarSym,
                                      String iban, String swift, String specSymbol,
                                      String account, String accountPre, String bankCode) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record Insurance(Double insurancePrice, String insuranceCurrency) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record ExternalNumber(String code, String externalNumber) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record Dormant(String shipmentNumber, String note, Address recipient,
                               List<ExternalNumber> externalNumbers, List<Service> services, WeighedShipmentInfo weighedShipmentInfo) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record Service(String code) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record ShipmentRouting(String inputRouteCode) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record DirectInjection(Boolean directAddressing, String gatewayZipCode, String gatewayCity, String country) {}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private record LabelService(Boolean labelless) {}
    }
}
