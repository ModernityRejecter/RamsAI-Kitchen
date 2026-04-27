package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.TableResponse;
import com.ramsai.kitchen.services.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
