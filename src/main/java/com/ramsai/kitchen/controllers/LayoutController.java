package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.repositories.RestaurantWallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/layout")
@RequiredArgsConstructor
public class LayoutController {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantWallRepository wallRepository;
    private final OrderRepository orderRepository;

    @DeleteMapping("/reset")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetLayout() {
        // Clear related orders first to prevent foreign key constraint violations
        orderRepository.deleteAll();
        tableRepository.deleteAll();
        wallRepository.deleteAll();
        
        return ResponseEntity.ok(Map.of(
                "message", "Layout reset successfully"
        ));
    }
}
