package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.models.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Query(
        "SELECT o FROM Order o WHERE o.table.id = :tableId " +
        "ORDER BY COALESCE(o.placedAt, o.createdAt) DESC"
    )
    List<Order> findAllByTableIdOrderByPlacedAtDesc(Long tableId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId AND o.status = :status")
    Optional<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT o FROM Order o " +
        "LEFT JOIN FETCH o.items i " +
        "LEFT JOIN FETCH i.product " +
        "LEFT JOIN FETCH o.table " +
        "WHERE o.customerId = :customerId AND o.status <> :excluded"
    )
    List<Order> findCustomerOrders(Long customerId, OrderStatus excluded);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT o FROM Order o " +
        "LEFT JOIN FETCH o.items i " +
        "LEFT JOIN FETCH i.product " +
        "LEFT JOIN FETCH o.table " +
        "WHERE o.status IN :statuses"
    )
    List<Order> findForKitchenByStatuses(List<OrderStatus> statuses);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT o FROM Order o " +
        "LEFT JOIN FETCH o.items i " +
        "LEFT JOIN FETCH i.product " +
        "LEFT JOIN FETCH o.table " +
        "WHERE o.id = :id"
    )
    Optional<Order> findByIdForKitchen(Long id);
}
