package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;
import java.util.List;

public record SalesReportResponse(
        long totalUnitsSold,
        BigDecimal totalRevenue,
        double overallAverageRating,
        long totalOrders,
        BigDecimal averageOrderValue,
        List<CategorySalesReport> categories,
        List<SalesReportRow> topProducts,
        List<DailySalesPoint> dailySales
) {}
