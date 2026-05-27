package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.InventoryChangeReason;
import com.ramsai.kitchen.exceptions.InsufficientStockException;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.models.entities.InventoryLog;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.ProductIngredient;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.InventoryLogRepository;
import com.ramsai.kitchen.repositories.ProductIngredientRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final IngredientRepository ingredientRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;

    @Transactional
    public void validateAndDeductForOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new RuntimeException("Cannot process inventory for empty order.");
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProductId()));
            deductStockForProduct(product, item.getQuantity());
        }
    }

    @Transactional
    public void deductStockForProduct(Product product, Integer quantity) {
        log.info("Deducting stock for product: {} x{}", product.getName(), quantity);

        List<ProductIngredient> productIngredients = productIngredientRepository.findAllByProductId(product.getId());
        if (productIngredients.isEmpty()) {
            throw new InsufficientStockException("Product has no ingredient configuration: " + product.getName());
        }

        for (ProductIngredient pi : productIngredients) {
            Ingredient ingredient = pi.getIngredient();
            Double amountNeeded = pi.getQuantityRequired() * quantity;
            if (ingredient.getCurrentStock() < amountNeeded) {
                throw new InsufficientStockException("Insufficient stock for ingredient: " + ingredient.getName());
            }
        }

        Set<Long> affectedIngredientIds = new HashSet<>();
        for (ProductIngredient pi : productIngredients) {
            Ingredient ingredient = pi.getIngredient();
            Double amountToDeduct = pi.getQuantityRequired() * quantity;

            ingredient.setCurrentStock(ingredient.getCurrentStock() - amountToDeduct);
            ingredientRepository.save(ingredient);
            affectedIngredientIds.add(ingredient.getId());

            InventoryLog logEntry = InventoryLog.builder()
                    .ingredient(ingredient)
                    .changeAmount(-amountToDeduct)
                    .reason(InventoryChangeReason.ORDER_CONSUMPTION)
                    .build();
            inventoryLogRepository.save(logEntry);
            
            log.info("Deducted {} {} from ingredient: {}", amountToDeduct, ingredient.getUnit(), ingredient.getName());
        }

        for (Long ingredientId : affectedIngredientIds) {
            recalculateAvailabilityForIngredient(ingredientId);
        }
    }

    private void recalculateAvailabilityForIngredient(Long ingredientId) {
        List<ProductIngredient> impactedRows = productIngredientRepository.findAllByIngredientId(ingredientId);
        Set<Long> impactedProductIds = impactedRows.stream()
                .map(pi -> pi.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (Long productId : impactedProductIds) {
            Product impactedProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            boolean canProduce = canProduceAtLeastOneUnit(impactedProduct);
            if (impactedProduct.isActive() != canProduce) {
                impactedProduct.setActive(canProduce);
                productRepository.save(impactedProduct);
            }
        }
    }

    private boolean canProduceAtLeastOneUnit(Product product) {
        List<ProductIngredient> recipeRows = productIngredientRepository.findAllByProductId(product.getId());
        if (recipeRows.isEmpty()) {
            return false;
        }
        for (ProductIngredient pi : recipeRows) {
            if (pi.getIngredient().getCurrentStock() < pi.getQuantityRequired()) {
                return false;
            }
        }
        return true;
    }
}
