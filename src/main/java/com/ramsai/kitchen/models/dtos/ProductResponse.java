package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal basePrice,
    boolean isSpecialOffer,
    boolean isDailyRecipe,
    BigDecimal discountPrice,
    Double averageRating,
    String categoryName,
    Long categoryId,
    String approvalStatus,
    boolean isActive,
    String rejectionFeedback
) {}
