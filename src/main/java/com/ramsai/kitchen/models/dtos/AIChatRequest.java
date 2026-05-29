package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record AIChatRequest(
    @NotBlank(message = "Message content cannot be empty")
    String content
) {}
