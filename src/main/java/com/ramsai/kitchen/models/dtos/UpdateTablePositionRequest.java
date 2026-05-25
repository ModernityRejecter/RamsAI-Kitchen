package com.ramsai.kitchen.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTablePositionRequest(
    @NotNull(message = "X position is required")
    @PositiveOrZero(message = "X position must be positive or zero")
    Integer xPos,

    @NotNull(message = "Y position is required")
    @PositiveOrZero(message = "Y position must be positive or zero")
    Integer yPos
) {}
