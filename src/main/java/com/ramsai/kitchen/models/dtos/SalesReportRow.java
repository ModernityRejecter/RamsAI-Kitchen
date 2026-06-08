package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;

public record SalesReportRow(
        Long productId,
        String productName,
        String categoryName,
        long quantitySold,
        BigDecimal revenue,
        double averageRating
) {}
