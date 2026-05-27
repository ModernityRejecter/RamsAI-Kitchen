package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.InventoryLogResponse;
import com.ramsai.kitchen.models.entities.InventoryLog;
import com.ramsai.kitchen.repositories.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private final InventoryLogRepository inventoryLogRepository;

    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getRecentLogs() {
        return inventoryLogRepository.findTop100ByOrderByTimestampDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InventoryLogResponse toResponse(InventoryLog log) {
        return new InventoryLogResponse(
                log.getId(),
                log.getIngredient().getId(),
                log.getIngredient().getName(),
                log.getChangeAmount(),
                log.getReason(),
                log.getTimestamp()
        );
    }
}
