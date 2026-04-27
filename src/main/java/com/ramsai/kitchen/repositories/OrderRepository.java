package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByTableIdOrderByCreatedAtDesc(Long tableId);
}
