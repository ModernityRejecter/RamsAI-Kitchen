package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.ItemStatus;
import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long orderId,
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    String specialNotes,
    ItemStatus itemStatus
) {}
