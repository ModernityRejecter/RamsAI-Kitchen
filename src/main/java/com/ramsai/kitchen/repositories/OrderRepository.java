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
    Optional<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
