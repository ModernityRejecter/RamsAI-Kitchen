package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.ProductIngredientResponse;
import com.ramsai.kitchen.models.dtos.ProductIngredientUpsertRequest;
import com.ramsai.kitchen.services.ProductIngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class ProductIngredientController {

    private final ProductIngredientService productIngredientService;

    @GetMapping("/products/{productId}/ingredients")
    public ResponseEntity<Map<String, Object>> getProductIngredients(@PathVariable Long productId) {
        List<ProductIngredientResponse> data = productIngredientService.getByProductId(productId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping("/products/{productId}/ingredients")
    public ResponseEntity<Map<String, Object>> addProductIngredient(
            @PathVariable Long productId,
            @Valid @RequestBody ProductIngredientUpsertRequest request) {
        ProductIngredientResponse data = productIngredientService.create(productId, request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product ingredient created successfully"
        ));
    }

    @PutMapping("/product-ingredients/{id}")
    public ResponseEntity<Map<String, Object>> updateProductIngredient(
            @PathVariable Long id,
            @Valid @RequestBody ProductIngredientUpsertRequest request) {
        ProductIngredientResponse data = productIngredientService.update(id, request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Product ingredient updated successfully"
        ));
    }

    @DeleteMapping("/product-ingredients/{id}")
    public ResponseEntity<Map<String, Object>> deleteProductIngredient(@PathVariable Long id) {
        productIngredientService.delete(id);
        return ResponseEntity.ok(Map.of(
                "message", "Product ingredient deleted successfully"
        ));
    }
}
