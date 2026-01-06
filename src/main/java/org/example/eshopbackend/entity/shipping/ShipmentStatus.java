package org.example.eshopbackend.entity.shipping;

// src/main/java/org/example/zeniqbackend/entity/shipping/ShipmentStatus.java


public enum ShipmentStatus {
    NEW,                // vytvořeno u nás, čeká na odeslání do PPL
    REQUESTED,          // odesláno do PPL (POST shipment/batch přijato)
    LABEL_READY,        // máme stažené etikety
    HANDED_OVER,        // předáno kurýrovi
    IN_TRANSIT,         // v přepravě
    DELIVERED,          // doručeno
    CANCELLED,          // zrušeno u PPL
    ERROR               // chyba (např. při volání API)
}

