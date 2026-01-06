package org.example.eshopbackend.adultoTest;

import org.example.eshopbackend.adulto.AdultoVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.example.eshopbackend.adulto.MockAdultoVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MockAdultoVerifierTest {
    AdultoVerifier v = new MockAdultoVerifier();

    @Test void okTrue() {
        assertTrue(v.verify("DEV-MOCK-UID-OK", 18).adult());
        assertTrue(v.verify("OK", 18).adult());
    }

    @Test
    void nokFalse() {
        assertFalse(v.verify("DEV-MOCK-UID-NOK", 18).adult());
        assertFalse(v.verify("NOK", 18).adult());
    }
}

