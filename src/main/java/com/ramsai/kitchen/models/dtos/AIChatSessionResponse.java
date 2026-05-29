package com.ramsai.kitchen.models.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record AIChatSessionResponse(
    Long id,
    LocalDateTime startedAt,
    String summary,
    List<AIMessageResponse> messages
) {}
