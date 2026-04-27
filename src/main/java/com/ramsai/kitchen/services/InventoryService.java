package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.InventoryChangeReason;
import com.ramsai.kitchen.exceptions.InsufficientStockException;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.models.entities.InventoryLog;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.ProductIngredient;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final IngredientRepository ingredientRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional
    public void deductStockForProduct(Product product, Integer quantity) {
        log.info("Deducting stock for product: {} x{}", product.getName(), quantity);
        
        // First check if all ingredients have enough stock
        for (ProductIngredient pi : product.getProductIngredients()) {
            Ingredient ingredient = pi.getIngredient();
            Double amountNeeded = pi.getQuantityRequired() * quantity;
            if (ingredient.getCurrentStock() < amountNeeded) {
                throw new InsufficientStockException("Insufficient stock for ingredient: " + ingredient.getName());
            }
        }

        for (ProductIngredient pi : product.getProductIngredients()) {
            Ingredient ingredient = pi.getIngredient();
            Double amountToDeduct = pi.getQuantityRequired() * quantity;
            
            ingredient.setCurrentStock(ingredient.getCurrentStock() - amountToDeduct);
            ingredientRepository.save(ingredient);

            InventoryLog logEntry = InventoryLog.builder()
                    .ingredient(ingredient)
                    .changeAmount(-amountToDeduct)
                    .reason(InventoryChangeReason.ORDER_CONSUMPTION)
                    .build();
            inventoryLogRepository.save(logEntry);
            
            log.info("Deducted {} {} from ingredient: {}", amountToDeduct, ingredient.getUnit(), ingredient.getName());
        }
    }
}
