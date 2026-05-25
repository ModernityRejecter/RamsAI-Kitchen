package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    Long orderId,
    List<OrderItemResponse> items,
    BigDecimal totalPrice
) {}
