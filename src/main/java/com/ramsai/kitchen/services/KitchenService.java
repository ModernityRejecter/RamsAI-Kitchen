package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.mappers.OrderItemMapper;
import com.ramsai.kitchen.mappers.OrderMapper;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenService {

    private static final List<OrderStatus> ACTIVE_KITCHEN_STATUSES =
            List.of(OrderStatus.RECEIVED, OrderStatus.COOKING, OrderStatus.READY);

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<OrderResponse> getBoard() {
        return orderRepository.findForKitchenByStatuses(ACTIVE_KITCHEN_STATUSES)
                .stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getKdsItems(ItemStatus status) {
        return orderItemRepository.findAllWithProductByItemStatusOrderByOrderCreatedAtAsc(status)
                .stream()
                .map(orderItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateItemStatus(Long itemId, ItemStatus newStatus) {
        OrderItem item = orderItemRepository.findByIdWithProduct(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + itemId));

        ItemStatus oldStatus = item.getItemStatus();
        item.setItemStatus(newStatus);
        orderItemRepository.save(item);
        log.info("Updated Item {} status from {} to {}", itemId, oldStatus, newStatus);

        Order order = loadOrder(item.getOrder().getId());
        return rollUpAndSave(order);
    }

    @Transactional
    public OrderResponse advanceOrder(Long orderId, OrderStatus targetStatus) {
        Order order = loadOrder(orderId);
        ItemStatus targetItemStatus = toItemStatus(targetStatus);
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (shouldAdvance(item.getItemStatus(), targetItemStatus)) {
                    item.setItemStatus(targetItemStatus);
                }
            }
        }
        order.setStatus(deriveOrderStatus(order));
        Order saved = orderRepository.save(order);
        log.info("Advanced Order {} to {}", orderId, saved.getStatus());
        return orderMapper.toResponse(saved);
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findByIdForKitchen(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponse rollUpAndSave(Order order) {
        order.setStatus(deriveOrderStatus(order));
        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    // Derives the order-level status from its items so customers see Received/Cooking/Ready/Served move forward.
    private OrderStatus deriveOrderStatus(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            return order.getStatus();
        }
        List<OrderItem> active = items.stream()
                .filter(i -> i.getItemStatus() != ItemStatus.CANCELLED)
                .collect(Collectors.toList());
        if (active.isEmpty()) {
            return OrderStatus.CANCELLED;
        }
        if (active.stream().allMatch(i -> i.getItemStatus() == ItemStatus.SERVED)) {
            return OrderStatus.SERVED;
        }
        if (active.stream().allMatch(i -> i.getItemStatus() == ItemStatus.READY || i.getItemStatus() == ItemStatus.SERVED)) {
            return OrderStatus.READY;
        }
        boolean anyStarted = active.stream().anyMatch(i ->
                i.getItemStatus() == ItemStatus.COOKING
                || i.getItemStatus() == ItemStatus.READY
                || i.getItemStatus() == ItemStatus.SERVED);
        return anyStarted ? OrderStatus.COOKING : OrderStatus.RECEIVED;
    }

    private ItemStatus toItemStatus(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case COOKING -> ItemStatus.COOKING;
            case READY -> ItemStatus.READY;
            case SERVED -> ItemStatus.SERVED;
            case CANCELLED -> ItemStatus.CANCELLED;
            default -> ItemStatus.PENDING;
        };
    }

    // Order-level actions only move items forward; already-finished items are left untouched.
    private boolean shouldAdvance(ItemStatus current, ItemStatus target) {
        if (current == ItemStatus.CANCELLED) {
            return false;
        }
        if (target == ItemStatus.CANCELLED) {
            return true;
        }
        return rank(current) < rank(target);
    }

    private int rank(ItemStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case COOKING -> 1;
            case READY -> 2;
            case SERVED -> 3;
            case CANCELLED -> -1;
        };
    }
}
