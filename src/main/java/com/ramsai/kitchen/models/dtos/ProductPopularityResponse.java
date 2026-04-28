package com.ramsai.kitchen.models.dtos;

public record ProductPopularityResponse(
    Long id,
    String name,
    Long totalQuantitySold,
    String categoryName
) {}
