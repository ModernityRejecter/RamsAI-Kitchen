package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.InventoryChangeReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockAdjustmentRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Double quantity,
        @NotNull(message = "Reason is required")
        InventoryChangeReason reason
) {
}
