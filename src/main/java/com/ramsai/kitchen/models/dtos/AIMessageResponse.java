package com.ramsai.kitchen.models.dtos;

import com.ramsai.kitchen.enums.SenderType;
import java.time.LocalDateTime;

public record AIMessageResponse(
    Long id,
    SenderType senderType,
    String content,
    LocalDateTime timestamp
) {}
