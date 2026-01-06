package org.example.eshopbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("ppl")
public class PplProps {
    private String apiBase;
    private Oauth oauth = new Oauth();
    private Sender sender = new Sender();
    private AgeCheck ageCheck = new AgeCheck();

    @Data
    public static class Oauth {
        private String tokenUrl;
        private String clientId;
        private String clientSecret;
        private String scope;
    }

    @Data
    public static class Sender {
        private String name;
        private String street;
        private String city;
        private String zipCode;
        private String country;
        private String contact;
        private String phone;
        private String email;
        private String depot;
        private String integratorId;
    }

    @Data
    public static class AgeCheck {
        private boolean enabled = true;
        /** Příklad: "A18" nebo "18" (pak se prefiksne na "A18"). */
        private String code = "A18";
    }
}
