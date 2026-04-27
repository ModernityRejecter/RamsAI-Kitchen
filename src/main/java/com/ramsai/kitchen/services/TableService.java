package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.models.dtos.TableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTablesWithLastOrderTime() {
        return tableRepository.findAll().stream()
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    private TableResponse mapToTableResponse(RestaurantTable table) {
        List<Order> orders = orderRepository.findAllByTableIdOrderByCreatedAtDesc(table.getId());
        LocalDateTime lastOrderTime = orders.isEmpty() ? null : orders.get(0).getCreatedAt();

        return new TableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getStatus(),
                table.getXPos(),
                table.getYPos(),
                lastOrderTime
        );
    }
}
