package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.AnalyticsAnswerResponse;
import com.ramsai.kitchen.models.dtos.AnalyticsQuestionRequest;
import com.ramsai.kitchen.models.dtos.SalesReportResponse;
import com.ramsai.kitchen.services.ManagerAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/analytics")
@RequiredArgsConstructor
public class ManagerAnalyticsController {

    private final ManagerAnalyticsService analyticsService;

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getReport() {
        SalesReportResponse report = analyticsService.getSalesReport();
        return ResponseEntity.ok(Map.of("data", report, "message", "Sales report generated"));
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@Valid @RequestBody AnalyticsQuestionRequest request) {
        AnalyticsAnswerResponse answer = analyticsService.ask(request.question());
        return ResponseEntity.ok(Map.of("data", answer, "message", "Answer generated"));
    }
}
