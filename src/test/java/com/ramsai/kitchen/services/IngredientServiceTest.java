package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.InventoryChangeReason;
import com.ramsai.kitchen.models.dtos.IngredientUpsertRequest;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.repositories.InventoryLogRepository;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.ProductIngredientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private ProductIngredientRepository productIngredientRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @InjectMocks
    private IngredientService ingredientService;

    @Test
    void create_DuplicateNameThrowsException() {
        IngredientUpsertRequest request = new IngredientUpsertRequest("Salt", "g", 10.0, 2.0);
        when(ingredientRepository.existsByNameIgnoreCase("Salt")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> ingredientService.create(request));
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void delete_IngredientLinkedToProductsThrowsException() {
        Ingredient ingredient = Ingredient.builder().id(1L).name("Salt").unit("g").currentStock(10.0).minimumStockThreshold(2.0).build();
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
        when(productIngredientRepository.existsByIngredientId(1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> ingredientService.delete(1L));
        verify(ingredientRepository, never()).delete(any());
    }

    @Test
    void adjustStock_OrderConsumptionReasonRejected() {
        assertThrows(RuntimeException.class,
                () -> ingredientService.adjustStock(1L, 10.0, InventoryChangeReason.ORDER_CONSUMPTION));
    }
}
