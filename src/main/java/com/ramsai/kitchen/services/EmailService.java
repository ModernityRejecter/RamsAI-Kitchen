package com.ramsai.kitchen.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendEmail(String to, String subject, String body) {
        // Mock email sending by printing to logs
        log.info("--------------------------------------------------");
        log.info("MOCK EMAIL SENT TO: {}", to);
        log.info("SUBJECT: {}", subject);
        log.info("BODY: \n{}", body);
        log.info("--------------------------------------------------");
    }

    public void sendPasswordResetEmail(String to, String token) {
        String body = "You requested a password reset. Use the following token to reset your password: \n" +
                token + "\n" +
                "Or click here (placeholder): http://localhost:8080/reset-password.html?token=" + token;
        sendEmail(to, "Password Reset Request", body);
    }

    public void sendEmailVerificationEmail(String to, String token) {
        String body = "Please verify your email by using the following token: \n" +
                token + "\n" +
                "Or click here (placeholder): http://localhost:8080/verify-email.html?token=" + token;
        sendEmail(to, "Email Verification", body);
    }
}
