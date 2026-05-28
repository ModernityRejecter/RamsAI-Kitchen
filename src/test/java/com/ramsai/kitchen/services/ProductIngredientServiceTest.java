package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.ProductIngredientUpsertRequest;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.ProductIngredient;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.ProductIngredientRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductIngredientServiceTest {

    @Mock
    private ProductIngredientRepository productIngredientRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private ProductIngredientService productIngredientService;

    @Test
    void create_shouldSetProductToPending() {
        Long productId = 1L;
        Product product = new Product();
        product.setId(productId);
        product.setApprovalStatus(Product.ApprovalStatus.APPROVED);

        Ingredient ingredient = new Ingredient();
        ingredient.setId(2L);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(ingredientRepository.findById(2L)).thenReturn(Optional.of(ingredient));
        when(productIngredientRepository.findAllByProductId(productId)).thenReturn(java.util.Collections.emptyList());

        ProductIngredient savedProductIngredient = new ProductIngredient();
        savedProductIngredient.setProduct(product);
        savedProductIngredient.setIngredient(ingredient);
        when(productIngredientRepository.save(any(ProductIngredient.class))).thenReturn(savedProductIngredient);

        productIngredientService.create(productId, new ProductIngredientUpsertRequest(2L, 100.0));

        assertEquals(Product.ApprovalStatus.PENDING, product.getApprovalStatus());
        verify(productRepository).save(product);
    }

    @Test
    void delete_shouldSetProductToPending() {
        Long productIngredientId = 1L;
        Product product = new Product();
        product.setApprovalStatus(Product.ApprovalStatus.APPROVED);
        
        ProductIngredient productIngredient = new ProductIngredient();
        productIngredient.setId(productIngredientId);
        productIngredient.setProduct(product);

        when(productIngredientRepository.findById(productIngredientId)).thenReturn(Optional.of(productIngredient));

        productIngredientService.delete(productIngredientId);

        assertEquals(Product.ApprovalStatus.PENDING, product.getApprovalStatus());
        verify(productRepository).save(product);
        verify(productIngredientRepository).delete(productIngredient);
    }
}
