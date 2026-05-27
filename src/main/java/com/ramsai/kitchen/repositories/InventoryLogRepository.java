package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findTop100ByOrderByTimestampDesc();
}
