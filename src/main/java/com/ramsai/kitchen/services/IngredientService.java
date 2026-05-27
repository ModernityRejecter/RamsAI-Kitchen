package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.InventoryChangeReason;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.models.dtos.IngredientResponse;
import com.ramsai.kitchen.models.dtos.IngredientUpsertRequest;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.models.entities.InventoryLog;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.InventoryLogRepository;
import com.ramsai.kitchen.repositories.ProductIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAll() {
        return ingredientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngredientResponse getById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));
        return toResponse(ingredient);
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getLowStock() {
        return ingredientRepository.findAll().stream()
                .filter(ingredient -> ingredient.getCurrentStock() <= ingredient.getMinimumStockThreshold())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IngredientResponse create(IngredientUpsertRequest request) {
        if (ingredientRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new RuntimeException("Ingredient already exists with name: " + request.name());
        }

        Ingredient ingredient = Ingredient.builder()
                .name(request.name().trim())
                .unit(request.unit().trim())
                .currentStock(request.currentStock())
                .minimumStockThreshold(request.minimumStockThreshold())
                .build();
        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse update(Long id, IngredientUpsertRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        ingredientRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Ingredient already exists with name: " + request.name());
                    }
                });

        ingredient.setName(request.name().trim());
        ingredient.setUnit(request.unit().trim());
        ingredient.setCurrentStock(request.currentStock());
        ingredient.setMinimumStockThreshold(request.minimumStockThreshold());
        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        if (productIngredientRepository.existsByIngredientId(id)) {
            throw new RuntimeException("Cannot delete ingredient used by one or more products.");
        }

        ingredientRepository.delete(ingredient);
    }

    @Transactional
    public IngredientResponse adjustStock(Long id, Double quantity, InventoryChangeReason reason) {
        if (reason == InventoryChangeReason.ORDER_CONSUMPTION) {
            throw new RuntimeException("ORDER_CONSUMPTION cannot be used for manual stock adjustments.");
        }

        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        double signedChange = reason == InventoryChangeReason.WASTE ? -quantity : quantity;
        double newStock = ingredient.getCurrentStock() + signedChange;
        if (newStock < 0) {
            throw new RuntimeException("Stock adjustment would make current stock negative.");
        }

        ingredient.setCurrentStock(newStock);
        ingredientRepository.save(ingredient);

        InventoryLog logEntry = InventoryLog.builder()
                .ingredient(ingredient)
                .changeAmount(signedChange)
                .reason(reason)
                .build();
        inventoryLogRepository.save(logEntry);

        return toResponse(ingredient);
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnit(),
                ingredient.getCurrentStock(),
                ingredient.getMinimumStockThreshold()
        );
    }
}
