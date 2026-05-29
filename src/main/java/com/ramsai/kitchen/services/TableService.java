package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.repositories.RestaurantWallRepository;
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
    private final RestaurantWallRepository wallRepository;
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
    public TableResponse addTable(Integer xPos, Integer yPos) {
        if (xPos == null || yPos == null) {
            throw new RuntimeException("Position is required");
        }
        if (tableRepository.findByXposAndYpos(xPos, yPos).isPresent() ||
                wallRepository.findByXposAndYpos(xPos, yPos).isPresent()) {
            throw new RuntimeException("Position already occupied");
        }
        Integer newTableNumber = findSmallestAvailableTableNumber();
        RestaurantTable newTable = new RestaurantTable();
        newTable.setTableNumber(newTableNumber);
        newTable.setStatus(com.ramsai.kitchen.enums.TableStatus.FREE);
        newTable.setXpos(xPos);
        newTable.setYpos(yPos);
        RestaurantTable saved = tableRepository.save(newTable);
        return mapToTableResponse(saved);
    }

    @Transactional
    public void deleteTableSquare(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (table.getStatus() == com.ramsai.kitchen.enums.TableStatus.OCCUPIED) {
            throw new RuntimeException("Cannot delete an occupied table. Free it first.");
        }

        Integer tableNumber = table.getTableNumber();
        tableRepository.delete(table);

        List<RestaurantTable> remaining = tableRepository.findAll().stream()
                .filter(t -> t.getTableNumber().equals(tableNumber))
                .collect(Collectors.toList());

        if (remaining.size() > 1) {
            List<List<RestaurantTable>> components = findConnectedComponents(remaining);
            if (components.size() > 1) {
                components.sort((a, b) -> b.size() - a.size());
                for (int i = 1; i < components.size(); i++) {
                    Integer newNumber = findSmallestAvailableTableNumber();
                    for (RestaurantTable t : components.get(i)) {
                        t.setTableNumber(newNumber);
                        tableRepository.save(t);
                    }
                }
            }
        }
    }

    @Transactional
    public TableResponse updateTablePosition(Long id, Integer xPos, Integer yPos) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        tableRepository.findByXposAndYpos(xPos, yPos).ifPresent(otherTable -> {
            if (!otherTable.getId().equals(id)) {
                throw new RuntimeException("Another table already exists at this position");
            }
        });

        wallRepository.findByXposAndYpos(xPos, yPos).ifPresent(wall -> {
            throw new RuntimeException("A wall already exists at this position");
        });

        Integer oldTableNumber = table.getTableNumber();

        // Find which group members remain after this piece is removed
        List<RestaurantTable> oldGroupMembers = tableRepository.findAll().stream()
                .filter(t -> !t.getId().equals(id) && t.getTableNumber().equals(oldTableNumber))
                .collect(Collectors.toList());
        boolean sharedBefore = !oldGroupMembers.isEmpty();

        table.setXpos(xPos);
        table.setYpos(yPos);

        // If dragging away splits the remaining pieces into disconnected components,
        // assign new table numbers to each disconnected fragment.
        if (sharedBefore) {
            List<List<RestaurantTable>> components = findConnectedComponents(oldGroupMembers);

            if (components.size() > 1) {
                // Largest component keeps the original table number; others get new ones.
                components.sort((a, b) -> b.size() - a.size());

                for (int i = 1; i < components.size(); i++) {
                    Integer newNumber = findSmallestAvailableTableNumber();
                    for (RestaurantTable t : components.get(i)) {
                        t.setTableNumber(newNumber);
                        tableRepository.save(t);
                    }
                }
            }
        }

        // Detect adjacent tables at the new position
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

        Integer effectiveDraggedNumber = table.getTableNumber();
        if (sharedBefore) {
            // Broke away from old group — needs a fresh number before merging with neighbors
            effectiveDraggedNumber = findSmallestAvailableTableNumber();
        }

        if (!neighbors.isEmpty()) {
            Integer minNeighborNumber = neighbors.stream()
                .map(RestaurantTable::getTableNumber)
                .min(Integer::compareTo)
                .orElse(effectiveDraggedNumber);

            Integer finalNumber = Math.min(effectiveDraggedNumber, minNeighborNumber);

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
            if (sharedBefore) {
                table.setTableNumber(effectiveDraggedNumber);
            }
        }

        RestaurantTable savedTable = tableRepository.save(table);
        return mapToTableResponse(savedTable);
    }

    private List<List<RestaurantTable>> findConnectedComponents(List<RestaurantTable> tables) {
        List<List<RestaurantTable>> components = new java.util.ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();

        java.util.Map<String, RestaurantTable> positionMap = new java.util.HashMap<>();
        for (RestaurantTable t : tables) {
            positionMap.put(t.getXpos() + "," + t.getYpos(), t);
        }

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (RestaurantTable start : tables) {
            if (visited.contains(start.getId())) continue;

            List<RestaurantTable> component = new java.util.ArrayList<>();
            java.util.Queue<RestaurantTable> queue = new java.util.LinkedList<>();
            queue.add(start);
            visited.add(start.getId());

            while (!queue.isEmpty()) {
                RestaurantTable current = queue.poll();
                component.add(current);

                for (int[] dir : directions) {
                    String key = (current.getXpos() + dir[0]) + "," + (current.getYpos() + dir[1]);
                    RestaurantTable neighbor = positionMap.get(key);
                    if (neighbor != null && !visited.contains(neighbor.getId())) {
                        visited.add(neighbor.getId());
                        queue.add(neighbor);
                    }
                }
            }

            components.add(component);
        }

        return components;
    }

    @Transactional
    public TableResponse freeTable(Long id) {
        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        List<RestaurantTable> group = tableRepository.findAll().stream()
                .filter(t -> t.getTableNumber().equals(table.getTableNumber()))
                .collect(Collectors.toList());

        group.forEach(t -> {
            t.setStatus(com.ramsai.kitchen.enums.TableStatus.FREE);
            t.setOccupiedByUserId(null);
            t.setOccupiedAt(null);
        });
        tableRepository.saveAll(group);
        return mapToTableResponse(table);
    }

    @Transactional
    public TableResponse occupyTable(Long id, Long userId) {
        boolean alreadyOccupied = tableRepository.findAll().stream()
                .anyMatch(t -> userId.equals(t.getOccupiedByUserId()));
        if (alreadyOccupied) {
            throw new RuntimeException("You are already occupying a table. Free it first.");
        }

        RestaurantTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        List<RestaurantTable> group = tableRepository.findAll().stream()
                .filter(t -> t.getTableNumber().equals(table.getTableNumber()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        group.forEach(t -> {
            t.setStatus(com.ramsai.kitchen.enums.TableStatus.OCCUPIED);
            t.setOccupiedByUserId(userId);
            t.setOccupiedAt(now);
        });
        tableRepository.saveAll(group);
        return mapToTableResponse(table);
    }

    private TableResponse mapToTableResponse(RestaurantTable table) {
        List<Order> orders = orderRepository.findAllByTableIdOrderByPlacedAtDesc(table.getId());
        LocalDateTime lastOrderTime = orders.isEmpty() ? null
                : (orders.get(0).getPlacedAt() != null ? orders.get(0).getPlacedAt() : orders.get(0).getCreatedAt());

        return new TableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getStatus(),
                table.getXpos(),
                table.getYpos(),
                lastOrderTime,
                table.getOccupiedByUserId(),
                table.getOccupiedAt()
        );
    }
}
