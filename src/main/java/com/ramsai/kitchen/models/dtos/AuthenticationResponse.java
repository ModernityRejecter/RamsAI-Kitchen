package com.ramsai.kitchen.models.dtos;

public record AuthenticationResponse(
    String token,
    String username,
    String role
) {}
