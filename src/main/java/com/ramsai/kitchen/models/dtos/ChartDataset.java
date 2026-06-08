package com.ramsai.kitchen.models.dtos;

import java.util.List;

public record ChartDataset(
        String label,
        List<Double> data
) {}
