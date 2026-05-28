package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.enums.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o JOIN FETCH oi.product p WHERE oi.itemStatus = :status ORDER BY o.createdAt ASC")
    List<OrderItem> findAllWithProductByItemStatusOrderByOrderCreatedAtAsc(ItemStatus status);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product p WHERE oi.id = :id")
    java.util.Optional<OrderItem> findByIdWithProduct(Long id);

    @Query("SELECT p.id, SUM(oi.quantity) as totalQty FROM OrderItem oi JOIN oi.product p GROUP BY p.id ORDER BY totalQty DESC")
    List<Object[]> findPopularProducts();

    boolean existsByProductId(Long productId);
}
