package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "Product name is required")
        String name,
        String description,
        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be positive")
        BigDecimal basePrice,
        @NotNull(message = "Category id is required")
        Long categoryId,
        Boolean isSpecialOffer,
        Boolean isDailyRecipe,
        BigDecimal discountPrice
) {
}
