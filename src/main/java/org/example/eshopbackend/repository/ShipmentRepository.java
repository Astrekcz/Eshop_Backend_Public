// src/main/java/org/example/zeniqbackend/repository/ShipmentRepository.java
package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.shipping.ShipmentEntity;
import org.example.eshopbackend.entity.shipping.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, Long> {
    Optional<ShipmentEntity> findByOrder_OrderNumber(String orderNumber);
    Optional<ShipmentEntity> findByTrackingNumber(String trackingNumber);
    Optional<ShipmentEntity> findByOrder(OrderEntity order);
    // ✅ používá ho ShipmentSyncJob
    Page<ShipmentEntity> findByStatusNotIn(Collection<ShipmentStatus> statuses, Pageable pageable);

}
