package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.AuditLog;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @Transactional
    public void logAction(User user, String action, String status, String details) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        AuditLog log = AuditLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : null)
                .action(action)
                .status(status)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional
    public void logFailedLogin(String username, String details) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        AuditLog log = AuditLog.builder()
                .username(username)
                .action("LOGIN_FAILURE")
                .status("FAILURE")
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
}
