package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.AIChatRequest;
import com.ramsai.kitchen.models.dtos.AIMessageResponse;
import com.ramsai.kitchen.models.entities.AIChatSession;
import com.ramsai.kitchen.models.entities.AIMessage;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@AuthenticationPrincipal User user) {
        AIChatSession session = aiChatService.createSession(user.getId());
        return ResponseEntity.ok(Map.of(
                "data", session,
                "message", "Session created successfully"
        ));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions(@AuthenticationPrincipal User user) {
        List<AIChatSession> sessions = aiChatService.getUserSessions(user.getId());
        return ResponseEntity.ok(Map.of(
                "data", sessions,
                "message", "Success"
        ));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(@PathVariable Long sessionId) {
        List<AIMessageResponse> messages = aiChatService.getMessages(sessionId).stream()
                .map(m -> new AIMessageResponse(m.getId(), m.getSenderType(), m.getContent(), m.getTimestamp()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "data", messages,
                "message", "Success"
        ));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody AIChatRequest request) {
        AIMessage response = aiChatService.sendMessage(sessionId, request.content());
        AIMessageResponse responseDto = new AIMessageResponse(
                response.getId(),
                response.getSenderType(),
                response.getContent(),
                response.getTimestamp()
        );
        return ResponseEntity.ok(Map.of(
                "data", responseDto,
                "message", "Success"
        ));
    }
}
