package org.example.eshopbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.service.email.EmailService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final EmailService emailService;

    // Jednoduché úložiště v paměti: Email -> Kód
    private final Map<String, String> codeStorage = new ConcurrentHashMap<>();

    public void sendCode(String email) {
        // 1. Vygenerovat 6místný kód
        String code = String.format("%06d", new Random().nextInt(999999));

        // 2. Uložit do mapy (převedeme na lowerCase, aby nezáleželo na velikosti písmen v emailu)
        codeStorage.put(email.trim().toLowerCase(), code);

        // 3. Odeslat přes existující EmailService
        emailService.sendVerificationCode(email, code);
    }

    public boolean verifyCode(String email, String codeToCheck) {
        if (email == null || codeToCheck == null) return false;

        String storedCode = codeStorage.get(email.trim().toLowerCase());

        // Pokud kód existuje a shoduje se
        if (storedCode != null && storedCode.equals(codeToCheck.trim())) {
            // Smazat kód, aby nešel použít znovu
            codeStorage.remove(email.trim().toLowerCase());
            return true;
        }
        return false;
    }
}