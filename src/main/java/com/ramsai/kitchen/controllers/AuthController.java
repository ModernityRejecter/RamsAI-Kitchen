package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.AuthenticationRequest;
import com.ramsai.kitchen.models.dtos.AuthenticationResponse;
import com.ramsai.kitchen.models.dtos.RegisterRequest;
import com.ramsai.kitchen.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authService.register(request);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "message", "User registered successfully"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authService.authenticate(request);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "message", "Login successful"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthenticationResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "message", "Token refreshed successfully"
        ));
    }
}
