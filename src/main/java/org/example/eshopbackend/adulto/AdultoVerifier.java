package org.example.eshopbackend.adulto;

public interface AdultoVerifier {
    AdultoVerification verify(String adultoUid, int requiredAge);
}
