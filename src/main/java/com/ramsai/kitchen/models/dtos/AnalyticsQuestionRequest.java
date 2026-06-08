package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record AnalyticsQuestionRequest(
        @NotBlank(message = "Question is required")
        String question
) {}
