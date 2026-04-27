package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.ProductPopularityResponse;
import com.ramsai.kitchen.models.dtos.ProductRatingResponse;
import com.ramsai.kitchen.services.ManagerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;

    @GetMapping("/popularity")
    public ResponseEntity<Map<String, Object>> getPopularityReport(@RequestParam(required = false) Long categoryId) {
        List<ProductPopularityResponse> data = dashboardService.getPopularityReport(categoryId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/ratings")
    public ResponseEntity<Map<String, Object>> getRatingsReport(@RequestParam(required = false) Long categoryId) {
        List<ProductRatingResponse> data = dashboardService.getRatingsReport(categoryId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }
}
