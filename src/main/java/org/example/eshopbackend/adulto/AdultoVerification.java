package org.example.eshopbackend.adulto;

public record AdultoVerification(
        boolean status,           // ověření proběhlo
        boolean adult,            // splněn požadovaný věk
        String requiredAge,       // např. "18"
        String method,            // MOCK (v dev), později BANKID/MOJEID/OCRID
        String verifyUid          // token/uid (v dev simulovaný)
) {}
