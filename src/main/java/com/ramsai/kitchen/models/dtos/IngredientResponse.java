package com.ramsai.kitchen.models.dtos;

public record IngredientResponse(
        Long id,
        String name,
        String unit,
        Double currentStock,
        Double minimumStockThreshold
) {
}
