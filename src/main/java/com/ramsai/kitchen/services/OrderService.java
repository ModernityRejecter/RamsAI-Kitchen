package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.mappers.OrderMapper;
import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long customerId) {
        return orderRepository.findCustomerOrders(customerId, OrderStatus.DRAFT)
                .stream()
                .sorted(Comparator.comparing(Order::effectivePlacedAt).reversed())
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }
}
