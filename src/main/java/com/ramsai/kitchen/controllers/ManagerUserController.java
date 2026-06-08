package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.UserResponse;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/users")
@RequiredArgsConstructor
public class ManagerUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(Map.of(
                "data", users,
                "message", "Success"
        ));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User caller) {
        String role = request.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
        }
        try {
            userService.updateUserRole(id, role, caller);
            return ResponseEntity.ok(Map.of("message", "User role updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
