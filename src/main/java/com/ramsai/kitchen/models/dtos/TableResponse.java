package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.TableStatus;
import java.time.LocalDateTime;

public record TableResponse(
    Long id,
    Integer tableNumber,
    TableStatus status,
    Integer xPos,
    Integer yPos,
    LocalDateTime lastOrderTime
) {}
