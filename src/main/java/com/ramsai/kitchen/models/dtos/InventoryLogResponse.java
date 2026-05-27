package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.InventoryChangeReason;

import java.time.LocalDateTime;

public record InventoryLogResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        Double changeAmount,
        InventoryChangeReason reason,
        LocalDateTime timestamp
) {
}
