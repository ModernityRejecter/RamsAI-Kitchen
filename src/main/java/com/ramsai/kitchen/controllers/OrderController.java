package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyOrders(@AuthenticationPrincipal User user) {
        List<OrderResponse> data = orderService.getMyOrders(user.getId());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }
}
