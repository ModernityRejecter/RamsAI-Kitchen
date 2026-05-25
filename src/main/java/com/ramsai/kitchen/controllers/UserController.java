package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        userService.requestPasswordReset(request.get("email"));
        return ResponseEntity.ok(Map.of("message", "If an account exists with that email, a reset link has been sent."));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        userService.resetPassword(request.get("token"), request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @PostMapping("/verify-email/request")
    public ResponseEntity<Map<String, Object>> requestVerification(@AuthenticationPrincipal User user) {
        userService.requestEmailVerification(user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Verification email sent."));
    }

    @PostMapping("/verify-email/confirm")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> request) {
        userService.verifyEmail(request.get("token"));
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        userService.updateProfile(user.getUsername(), request.get("email"), request.get("password"));
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Object>> deleteAccount(@AuthenticationPrincipal User user) {
        userService.softDeleteAccount(user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Account has been deactivated."));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "isEmailVerified", user.isEmailVerified(),
                        "createdAt", user.getCreatedAt()
                ),
                "message", "Success"
        ));
    }
}
