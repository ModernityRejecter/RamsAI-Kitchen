package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.CartItemRequest;
import com.ramsai.kitchen.models.dtos.CartResponse;
import com.ramsai.kitchen.models.dtos.OrderResponse;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.CartService;
import com.ramsai.kitchen.services.KitchenBroadcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final KitchenBroadcastService kitchenBroadcastService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCart(@AuthenticationPrincipal User user) {
        CartResponse data = cartService.getCart(user.getId());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse data = cartService.addItem(user.getId(), request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Item added to cart"
        ));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId) {
        CartResponse data = cartService.removeItem(user.getId(), itemId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Item removed from cart"
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @AuthenticationPrincipal User user,
            @RequestParam Long tableId) {
        OrderResponse order = cartService.checkout(user.getId(), tableId);
        kitchenBroadcastService.broadcastOrderUpdate(order);
        return ResponseEntity.ok(Map.of(
                "message", "Checkout successful. Your order is being prepared!"
        ));
    }
}
