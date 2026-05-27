package com.ramsai.kitchen.models.dtos;

public record ProductIngredientResponse(
        Long id,
        Long productId,
        Long ingredientId,
        String ingredientName,
        String ingredientUnit,
        Double quantityRequired
) {
}
