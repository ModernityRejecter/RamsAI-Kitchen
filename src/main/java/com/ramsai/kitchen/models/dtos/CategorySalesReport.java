package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CategorySalesReport(
        String categoryName,
        long totalQuantitySold,
        BigDecimal totalRevenue,
        List<SalesReportRow> products
) {}
