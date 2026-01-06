package org.example.eshopbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payments.bank")
@Getter
@Setter
public class PaymentProps {
    private String iban;
    private String bic;
}