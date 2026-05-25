package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
    @NotNull(message = "Product ID is required")
    Long productId,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity,
    
    String specialNotes
) {}
