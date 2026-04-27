package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.services.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> getKdsItems(@RequestParam ItemStatus status) {
        List<OrderItemResponse> data = kitchenService.getKdsItems(status);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PatchMapping("/items/{id}/status")
    public ResponseEntity<Map<String, Object>> updateItemStatus(
            @PathVariable Long id,
            @RequestParam ItemStatus status) {
        kitchenService.updateItemStatus(id, status);
        return ResponseEntity.ok(Map.of(
                "message", "Item status updated successfully"
        ));
    }
}
