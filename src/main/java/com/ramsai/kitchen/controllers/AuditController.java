package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.entities.AuditLog;
import com.ramsai.kitchen.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
                "data", logs.getContent(),
                "totalPages", logs.getTotalPages(),
                "totalElements", logs.getTotalElements(),
                "message", "Success"
        ));
    }
}
