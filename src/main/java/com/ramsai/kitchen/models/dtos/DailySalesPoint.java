package com.ramsai.kitchen.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesPoint(
        LocalDate date,
        long units,
        BigDecimal revenue,
        long orders
) {}
