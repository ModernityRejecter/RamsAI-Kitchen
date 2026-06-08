package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager/categories")
@RequiredArgsConstructor
public class ManagerCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        return ResponseEntity.ok(Map.of("data", categoryService.getAll(), "message", "Success"));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of("data", categoryService.create(request.get("name"), request.get("description")), "message", "Category created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of("data", categoryService.update(id, request.get("name"), request.get("description")), "message", "Category updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }
}
