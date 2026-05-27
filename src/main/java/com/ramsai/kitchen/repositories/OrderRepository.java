package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.models.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByTableIdOrderByCreatedAtDesc(Long tableId);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId AND o.status = :status")
    Optional<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
