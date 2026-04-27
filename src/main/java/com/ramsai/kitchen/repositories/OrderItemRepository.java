package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.enums.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o WHERE oi.itemStatus = :status ORDER BY o.createdAt ASC")
    List<OrderItem> findAllByItemStatusOrderByOrderCreatedAtAsc(ItemStatus status);

    @Query("SELECT oi.productId, SUM(oi.quantity) as totalQty FROM OrderItem oi GROUP BY oi.productId ORDER BY totalQty DESC")
    List<Object[]> findPopularProducts();
}
