package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.IngredientResponse;
import com.ramsai.kitchen.models.dtos.IngredientUpsertRequest;
import com.ramsai.kitchen.models.dtos.StockAdjustmentRequest;
import com.ramsai.kitchen.services.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllIngredients() {
        List<IngredientResponse> data = ingredientService.getAll();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getIngredientById(@PathVariable Long id) {
        IngredientResponse data = ingredientService.getById(id);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockIngredients() {
        List<IngredientResponse> data = ingredientService.getLowStock();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createIngredient(@Valid @RequestBody IngredientUpsertRequest request) {
        IngredientResponse data = ingredientService.create(request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Ingredient created successfully"
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody IngredientUpsertRequest request) {
        IngredientResponse data = ingredientService.update(id, request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Ingredient updated successfully"
        ));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> adjustIngredientStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        IngredientResponse data = ingredientService.adjustStock(id, request.quantity(), request.reason());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Ingredient stock adjusted successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteIngredient(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResponseEntity.ok(Map.of(
                "message", "Ingredient deleted successfully"
        ));
    }
}
