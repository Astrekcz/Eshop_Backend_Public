package org.example.eshopbackend.dto.adulto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdultoApiResponse(
        @JsonProperty("adultocz-verify-id")     String verifyId,
        @JsonProperty("adultocz-verify-uid")    String verifyUid,
        @JsonProperty("adultocz-visit-cookie")  String visitCookie,
        @JsonProperty("adultocz-visit-time")    String visitTime,
        @JsonProperty("adultocz-verify-time")   String verifyTime,
        @JsonProperty("adultocz-verify-method") String verifyMethod,   // např. bankid
        @JsonProperty("adultocz-verify-status") String verifyStatus,   // "true"/"false"
        @JsonProperty("adultocz-verify-age")    String verifyAge,      // "18"
        @JsonProperty("adultocz-verify-adult")  String verifyAdult     // "true"/"false"
) {}