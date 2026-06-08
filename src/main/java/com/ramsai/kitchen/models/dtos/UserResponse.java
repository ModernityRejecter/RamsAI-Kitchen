package com.ramsai.kitchen.models.dtos;

public record UserResponse(
    Long id,
    String username,
    String email,
    String role,
    boolean isActive
) {}
