package com.ramsai.kitchen.models.dtos;

import java.util.List;

public record ChartSpec(
        String type,
        String title,
        List<String> labels,
        List<ChartDataset> datasets
) {}
