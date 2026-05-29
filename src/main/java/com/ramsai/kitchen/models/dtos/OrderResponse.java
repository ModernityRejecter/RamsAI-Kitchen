package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long tableId,
    Integer tableNumber,
    OrderStatus status,
    BigDecimal totalPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime placedAt,
    List<OrderItemResponse> items
) {}
