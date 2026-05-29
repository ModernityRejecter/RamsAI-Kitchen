package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.enums.ItemStatus;
import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.models.dtos.OrderItemResponse;
import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.services.KitchenBroadcastService;
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
    private final KitchenBroadcastService kitchenBroadcastService;

    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> getBoard() {
        List<OrderResponse> data = kitchenService.getBoard();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

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
        OrderResponse order = kitchenService.updateItemStatus(id, status);
        kitchenBroadcastService.broadcastOrderUpdate(order);
        return ResponseEntity.ok(Map.of(
                "data", order,
                "message", "Item status updated successfully"
        ));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        OrderResponse order = kitchenService.advanceOrder(id, status);
        kitchenBroadcastService.broadcastOrderUpdate(order);
        return ResponseEntity.ok(Map.of(
                "data", order,
                "message", "Order status updated successfully"
        ));
    }
}
