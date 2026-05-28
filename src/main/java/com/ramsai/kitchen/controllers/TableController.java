package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.TableResponse;
import com.ramsai.kitchen.models.dtos.UpdateTablePositionRequest;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping("/map")
    public ResponseEntity<Map<String, Object>> getTableMap() {
        List<TableResponse> data = tableService.getAllTablesWithLastOrderTime();
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addTable(
            @RequestBody(required = false) Map<String, Integer> body) {
        Integer xPos = body != null ? body.get("xPos") : null;
        Integer yPos = body != null ? body.get("yPos") : null;
        if (xPos == null || yPos == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "xPos and yPos are required"));
        }
        TableResponse data = tableService.addTable(xPos, yPos);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Table created successfully"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTableSquare(@PathVariable Long id) {
        tableService.deleteTableSquare(id);
        return ResponseEntity.ok(Map.of("message", "Table square deleted successfully"));
    }

    @PutMapping("/{id}/position")
    public ResponseEntity<Map<String, Object>> updateTablePosition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTablePositionRequest request) {
        TableResponse data = tableService.updateTablePosition(id, request.xPos(), request.yPos());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Table position updated successfully"
        ));
    }

    @PutMapping("/{id}/free")
    public ResponseEntity<Map<String, Object>> freeTable(@PathVariable Long id) {
        TableResponse data = tableService.freeTable(id);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Table is now free"
        ));
    }

    @PutMapping("/{id}/occupy")
    public ResponseEntity<Map<String, Object>> occupyTable(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        TableResponse data = tableService.occupyTable(id, user.getId());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Table is now occupied"
        ));
    }
}
