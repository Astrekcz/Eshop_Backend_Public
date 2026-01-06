// org/example/zeniqbackend/adulto/AdultoProperties.java
package org.example.eshopbackend.adulto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "adulto.api")
public class AdultoProperties {
    private String resultUrl;   // https://api.result.adulto.cz
    private String publicKey;
    private String privateKey;
    private int timeoutMs = 4000;
    private boolean debug = false;
}
