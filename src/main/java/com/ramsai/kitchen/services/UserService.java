package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.models.entities.UserToken;
import com.ramsai.kitchen.repositories.UserRepository;
import com.ramsai.kitchen.repositories.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            userTokenRepository.deleteByUserAndTokenType(user, UserToken.TokenType.PASSWORD_RESET);
            
            String token = UUID.randomUUID().toString();
            UserToken userToken = UserToken.builder()
                    .user(user)
                    .token(token)
                    .tokenType(UserToken.TokenType.PASSWORD_RESET)
                    .expiryDate(LocalDateTime.now().plusHours(24))
                    .build();
            
            userTokenRepository.save(userToken);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            auditLogService.logAction(user, "PASSWORD_RESET_REQUEST", "SUCCESS", "Password reset requested");
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        UserToken userToken = userTokenRepository.findByTokenAndTokenType(token, UserToken.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (userToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            userTokenRepository.delete(userToken);
            throw new RuntimeException("Reset token has expired");
        }

        User user = userToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        userTokenRepository.delete(userToken);
        auditLogService.logAction(user, "PASSWORD_RESET_CONFIRM", "SUCCESS", "Password reset successfully");
    }

    @Transactional
    public void requestEmailVerification(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        userTokenRepository.deleteByUserAndTokenType(user, UserToken.TokenType.EMAIL_VERIFICATION);
        
        String token = UUID.randomUUID().toString();
        UserToken userToken = UserToken.builder()
                .user(user)
                .token(token)
                .tokenType(UserToken.TokenType.EMAIL_VERIFICATION)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        
        userTokenRepository.save(userToken);
        emailService.sendEmailVerificationEmail(user.getEmail(), token);
        auditLogService.logAction(user, "EMAIL_VERIFICATION_REQUEST", "SUCCESS", "Verification email requested");
    }

    @Transactional
    public void verifyEmail(String token) {
        UserToken userToken = userTokenRepository.findByTokenAndTokenType(token, UserToken.TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        if (userToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            userTokenRepository.delete(userToken);
            throw new RuntimeException("Verification token has expired");
        }

        User user = userToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        
        userTokenRepository.delete(userToken);
        auditLogService.logAction(user, "EMAIL_VERIFICATION_CONFIRM", "SUCCESS", "Email verified successfully");
    }

    @Transactional
    public void softDeleteAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(false);
        userRepository.save(user);
        
        auditLogService.logAction(user, "ACCOUNT_DELETE", "SUCCESS", "Account soft-deleted");
    }

    @Transactional
    public void updateProfile(String username, String newEmail, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(newEmail);
            user.setEmailVerified(false);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        auditLogService.logAction(user, "PROFILE_UPDATE", "SUCCESS", "Profile updated");
    }

    @Transactional
    public void updateProfilePicture(String username, String url) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setProfilePictureUrl(url);
        userRepository.save(user);
        auditLogService.logAction(user, "PROFILE_PICTURE_UPDATE", "SUCCESS", "Profile picture updated");
    }
}
