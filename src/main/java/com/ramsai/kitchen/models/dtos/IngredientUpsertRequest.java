package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record IngredientUpsertRequest(
        @NotBlank(message = "Ingredient name is required")
        String name,
        @NotBlank(message = "Unit is required")
        String unit,
        @NotNull(message = "Current stock is required")
        @PositiveOrZero(message = "Current stock cannot be negative")
        Double currentStock,
        @NotNull(message = "Minimum stock threshold is required")
        @PositiveOrZero(message = "Minimum stock threshold cannot be negative")
        Double minimumStockThreshold
) {
}
