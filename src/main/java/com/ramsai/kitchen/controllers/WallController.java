package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.WallResponse;
import com.ramsai.kitchen.models.dtos.UpdateTablePositionRequest;
import com.ramsai.kitchen.services.WallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/walls")
@RequiredArgsConstructor
public class WallController {

    private final WallService wallService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllWalls() {
        List<WallResponse> data = wallService.getAllWalls();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addWall(
            @RequestBody(required = false) Map<String, Integer> body) {
        Integer xPos = body != null ? body.get("xPos") : null;
        Integer yPos = body != null ? body.get("yPos") : null;
        if (xPos == null || yPos == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "xPos and yPos are required"));
        }
        WallResponse data = wallService.addWall(xPos, yPos);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Wall created successfully"
        ));
    }

    @PutMapping("/{id}/position")
    public ResponseEntity<Map<String, Object>> updateWallPosition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTablePositionRequest request) {
        WallResponse data = wallService.updateWallPosition(id, request.xPos(), request.yPos());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Wall position updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWall(@PathVariable Long id) {
        wallService.deleteWall(id);
        return ResponseEntity.ok(Map.of("message", "Wall deleted successfully"));
    }
}
