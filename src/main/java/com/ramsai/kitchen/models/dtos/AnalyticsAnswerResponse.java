package com.ramsai.kitchen.models.dtos;

public record AnalyticsAnswerResponse(
        String answer,
        ChartSpec chart
) {}
