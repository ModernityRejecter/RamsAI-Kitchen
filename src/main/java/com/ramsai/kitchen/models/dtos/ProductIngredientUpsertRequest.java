package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductIngredientUpsertRequest(
        @NotNull(message = "Ingredient id is required")
        Long ingredientId,
        @NotNull(message = "Quantity required is required")
        @Positive(message = "Quantity required must be positive")
        Double quantityRequired
) {
}
