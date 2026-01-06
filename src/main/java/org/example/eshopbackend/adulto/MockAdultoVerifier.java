package org.example.eshopbackend.adulto;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// MockAdultoVerifier.java
@Service
@Profile({"dev","test"})
public class MockAdultoVerifier implements AdultoVerifier {
    @Override
    public AdultoVerification verify(String adultoUid, int requiredAge) {
        boolean isAdult = false;

        if (adultoUid != null) {
            String u = adultoUid.trim().toUpperCase();

            // 1) přesná shoda
            if ("OK".equals(u)) {
                isAdult = true;
            } else if ("NOK".equals(u)) {
                isAdult = false;
            }
            // 2) naše konvence "DEV-MOCK-UID-OK"/"DEV-MOCK-UID-NOK"
            else if (u.endsWith("-OK")) {
                isAdult = true;
            } else if (u.endsWith("-NOK")) {
                isAdult = false;
            }
            // 3) případně jiné tvary -> default: false
        }

        return new AdultoVerification(
                adultoUid != null,              // status
                isAdult,                        // adult
                String.valueOf(requiredAge),    // "18"
                "MOCK",                         // method
                adultoUid                       // uid
        );
    }
}
