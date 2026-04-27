package com.ramsai.kitchen.models.dtos;

public record ProductRatingResponse(
    Long productId,
    String productName,
    Double averageRating,
    String categoryName
) {}
