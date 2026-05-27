package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.InventoryLogResponse;
import com.ramsai.kitchen.services.InventoryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/logs")
@RequiredArgsConstructor
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecentInventoryLogs() {
        List<InventoryLogResponse> data = inventoryLogService.getRecentLogs();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }
}
