package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.AIChatRequest;
import com.ramsai.kitchen.models.dtos.AIChatSessionResponse;
import com.ramsai.kitchen.models.dtos.AIMessageResponse;
import com.ramsai.kitchen.services.AIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> startSession() {
        AIChatSessionResponse session = aiChatService.startNewSession();
        return ResponseEntity.ok(Map.of("data", session, "message", "New AI session started"));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getSessions() {
        List<AIChatSessionResponse> sessions = aiChatService.getUserSessions();
        return ResponseEntity.ok(Map.of("data", sessions, "message", "Sessions retrieved"));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody AIChatRequest request) {
        AIMessageResponse message = aiChatService.sendMessage(sessionId, request.content());
        return ResponseEntity.ok(Map.of("data", message, "message", "AI response received"));
    }
}
