package org.example.eshopbackend.repository;

import org.example.eshopbackend.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Optional<OrderEntity> findTopByOrderNumberStartingWithOrderByIdDesc(String prefix);
    
}
