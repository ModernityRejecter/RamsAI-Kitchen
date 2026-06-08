package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;
import java.util.List;

public record SalesReportResponse(
        long totalUnitsSold,
        BigDecimal totalRevenue,
        double overallAverageRating,
        List<CategorySalesReport> categories,
        List<SalesReportRow> topProducts
) {}
