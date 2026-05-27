package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
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
}
