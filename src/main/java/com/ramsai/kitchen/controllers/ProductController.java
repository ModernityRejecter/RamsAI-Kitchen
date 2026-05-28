package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.dtos.ProductUpdateRequest;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/manage")
    public ResponseEntity<Map<String, Object>> getAllProductsForManagement() {
        List<ProductResponse> data = productService.getAllProductsForManagement();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse data = productService.createProduct(request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product created successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse data = productService.updateProduct(id, request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product updated successfully"
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(@RequestParam(required = false) Long categoryId) {
        List<ProductResponse> data;
        if (categoryId != null) {
            data = productService.getProductsByCategory(categoryId);
        } else {
            data = productService.getAllActiveProducts();
        }
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailyRecipes() {
        List<ProductResponse> data = productService.getDailyRecipes();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/recommended")
    public ResponseEntity<Map<String, Object>> getRecommendations() {
        List<ProductResponse> data = productService.getRecommendations();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        List<CategoryResponse> data = productService.getAllCategories();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveProduct(@PathVariable Long id) {
        ProductResponse data = productService.approveProduct(id);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product approved successfully"
        ));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectProduct(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String feedback = request.get("feedback");
        ProductResponse data = productService.rejectProduct(id, feedback);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product rejected successfully"
        ));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Map<String, Object>> updateActiveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean isActive = request.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "isActive field is required"));
        }
        ProductResponse data = productService.updateProductActiveStatus(id, isActive);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product status updated successfully"
        ));
    }
}
