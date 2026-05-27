package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.mappers.OrderItemMapper;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerId) {
        return orderRepository.findCustomerOrders(customerId, OrderStatus.DRAFT)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
        RestaurantTable table = order.getTable();
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                    .map(orderItemMapper::toResponse)
                    .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                table != null ? table.getId() : null,
                table != null ? table.getTableNumber() : null,
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
