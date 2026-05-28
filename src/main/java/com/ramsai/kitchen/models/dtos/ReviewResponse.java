package com.ramsai.kitchen.models.dtos;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long productId,
    String productName,
    Long customerId,
    String customerName,
    Integer rating,
    String comment,
    LocalDateTime createdAt
) {}
