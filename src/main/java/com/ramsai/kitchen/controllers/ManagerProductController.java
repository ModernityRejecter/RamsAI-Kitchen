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

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setApprovalStatus(Product.ApprovalStatus.APPROVED);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of(
                "message", "Product approved successfully"
        ));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setApprovalStatus(Product.ApprovalStatus.REJECTED);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of(
                "message", "Product rejected successfully"
        ));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(true);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of(
                "message", "Product activated successfully"
        ));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of(
                "message", "Product deactivated successfully"
        ));
    }
}
