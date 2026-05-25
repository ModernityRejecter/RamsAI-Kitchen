package com.ramsai.kitchen.models.dtos;

public record AuthenticationResponse(
    String token,
    String refreshToken,
    String username,
    String role
) {}
