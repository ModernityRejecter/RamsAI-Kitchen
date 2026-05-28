package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.mappers.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/manager/products")
@RequiredArgsConstructor
public class ManagerProductController {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingProducts() {
        List<ProductResponse> data = productRepository.findAllByIsActiveTrue().stream()
                .filter(p -> p.getApprovalStatus() == Product.ApprovalStatus.PENDING)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

}
