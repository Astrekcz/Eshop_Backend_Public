package org.example.eshopbackend.entity;

public enum OrderStatus {
    NEW,         // zákazník objednal
    PAID,        // admin potvrdil platbu
    SHIPPED,     // vytvořená zásilka v PPL
    DELIVERED,   // volitelné, až doručeno
    CANCELED     // zrušená objednávka
}
