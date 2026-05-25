package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.models.dtos.TableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTablesWithLastOrderTime() {
        return tableRepository.findAll().stream()
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    private Integer findSmallestAvailableTableNumber() {
        List<Integer> usedNumbers = tableRepository.findAll().stream()
                .map(RestaurantTable::getTableNumber)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        int candidate = 1;
        for (Integer num : usedNumbers) {
            if (num == candidate) {
                candidate++;
            } else if (num > candidate) {
                break;
            }
        }
        return candidate;
    }

    @Transactional
    public TableResponse addTable() {
        Integer newTableNumber = findSmallestAvailableTableNumber();

        RestaurantTable newTable = new RestaurantTable();
        newTable.setTableNumber(newTableNumber);
        newTable.setStatus(com.ramsai.kitchen.enums.TableStatus.FREE);
        // Find first available free spot (0,0) or some generic spot, UI can handle drag
        newTable.setXpos(0);
        newTable.setYpos(0);
        
        // Ensure no overlap at 0,0, if overlap we could search, but for prototype let's just place it at 0,0.
        // Actually, we'll try to find a free spot
        int x = 0, y = 0;
        while (tableRepository.findByXposAndYpos(x, y).isPresent()) {
            x++;
            if (x > 14) {
                x = 0;
                y++;
            }
        }
        newTable.setXpos(x);
        newTable.setYpos(y);

        RestaurantTable saved = tableRepository.save(newTable);
        return mapToTableResponse(saved);
    }

    @Transactional
    public TableResponse updateTablePosition(Long id, Integer xPos, Integer yPos) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Check for overlap
        tableRepository.findByXposAndYpos(xPos, yPos).ifPresent(otherTable -> {
            if (!otherTable.getId().equals(id)) {
                throw new RuntimeException("Another table already exists at this position");
            }
        });

        table.setXpos(xPos);
        table.setYpos(yPos);
        
        // Grouping logic: detect adjacent tables (top, bottom, left, right)
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        List<RestaurantTable> neighbors = new java.util.ArrayList<>();
        
        for (int[] dir : directions) {
            int nx = xPos + dir[0];
            int ny = yPos + dir[1];
            java.util.Optional<RestaurantTable> neighborOpt = tableRepository.findByXposAndYpos(nx, ny);
            if (neighborOpt.isPresent() && !neighborOpt.get().getId().equals(id)) {
                neighbors.add(neighborOpt.get());
            }
        }

        // Check if dragged table was sharing its number with others before this move
        boolean sharedBefore = tableRepository.findAll().stream()
                .anyMatch(t -> !t.getId().equals(id) && t.getTableNumber().equals(table.getTableNumber()));

        Integer effectiveDraggedNumber = table.getTableNumber();
        if (sharedBefore) {
            // It breaks away from its old group, so it loses its right to impose its old number on the new group
            effectiveDraggedNumber = findSmallestAvailableTableNumber();
        }

        if (!neighbors.isEmpty()) {
            // Find the minimum number among the dragged table (effective) and all neighbors
            Integer minNeighborNumber = neighbors.stream()
                .map(RestaurantTable::getTableNumber)
                .min(Integer::compareTo)
                .orElse(effectiveDraggedNumber);
                
            Integer finalNumber = Math.min(effectiveDraggedNumber, minNeighborNumber);

            // If the neighbors' groups need to change to the final number
            java.util.Set<Integer> neighborNumbersToUpdate = neighbors.stream()
                .map(RestaurantTable::getTableNumber)
                .filter(n -> !n.equals(finalNumber))
                .collect(Collectors.toSet());

            if (!neighborNumbersToUpdate.isEmpty()) {
                List<RestaurantTable> tablesToUpdate = tableRepository.findAll().stream()
                    .filter(t -> neighborNumbersToUpdate.contains(t.getTableNumber()))
                    .collect(Collectors.toList());
                    
                for (RestaurantTable t : tablesToUpdate) {
                    t.setTableNumber(finalNumber);
                    tableRepository.save(t);
                }
            }
            
            table.setTableNumber(finalNumber);
        } else {
            // No neighbors
            if (sharedBefore) {
                table.setTableNumber(effectiveDraggedNumber);
            }
        }

        RestaurantTable savedTable = tableRepository.save(table);
        return mapToTableResponse(savedTable);
    }

    private TableResponse mapToTableResponse(RestaurantTable table) {
        List<Order> orders = orderRepository.findAllByTableIdOrderByCreatedAtDesc(table.getId());
        LocalDateTime lastOrderTime = orders.isEmpty() ? null : orders.get(0).getCreatedAt();

        return new TableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getStatus(),
                table.getXpos(),
                table.getYpos(),
                lastOrderTime
        );
    }
}
