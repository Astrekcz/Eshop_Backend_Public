package org.example.eshopbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


// src/main/java/.../entity/OrderEntity.java
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false, length = 32, unique = true)
    private String orderNumber;

    @Column(nullable = false, length = 64)
    private String customerFirstName;

    @Column(nullable = false, length = 64)
    private String customerLastName;

    @Column(nullable = false, length = 128)

    private String customerEmail;

    @Column(length = 32)
    private String customerPhone;

    @Column(nullable = false, length = 64)
    private String shipStreet;

    @Column(nullable = false, length = 16)
    private String shipHouseNumber;

    @Column(length = 16)
    private String shipOrientationNumber;

    @Column(nullable = false, length = 64)
    private String shipCity;

    @Column(nullable = false, length = 5)
    private String shipPostalCode;

    @Column(nullable = false, length = 2)
    private String shipCountryCode = "CZ";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private boolean ageConfirmed;

    @Column(nullable = false)
    private Long subtotalCzk;

    @Column(nullable = false)
    private Long shippingCzk;

    @Column(nullable = false)
    private Long totalCzk;

    // jen variabilní symbol
    @Column(length = 20)
    private String bankVs;

    // src/main/java/org/example/zeniqbackend/entity/OrderEntity.java
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();


    // PPL tracking
    @Column(length = 64)
    private String trackingNumber;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
