package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.dtos.ProductIngredientUpsertRequest;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.*;
import com.ramsai.kitchen.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ReproductionBugTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductIngredientService productIngredientService;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;

    @Test
    @WithMockUser(roles = "MANAGER")
    void reproduceMenuLoadFailure() {
        // Setup category
        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        // Create product - Should be APPROVED and ACTIVE for MANAGER
        ProductCreateRequest productReq = new ProductCreateRequest(
                "New Product", "Desc", BigDecimal.TEN, category.getId(), false, false, null
        );
        ProductResponse product = productService.createProduct(productReq);
        assertEquals("APPROVED", product.approvalStatus());
        assertTrue(product.isActive());

        // Create ingredient with enough stock
        Ingredient ingredient = new Ingredient();
        ingredient.setName("New Ingredient");
        ingredient.setUnit("g");
        ingredient.setCurrentStock(100.0);
        ingredient.setMinimumStockThreshold(10.0);
        ingredient = ingredientRepository.save(ingredient);

        // Add ingredient to product - should NOT change status
        productIngredientService.create(product.id(), new ProductIngredientUpsertRequest(ingredient.getId(), 5.0));

        // Verify product remains APPROVED and active
        Product p = productRepository.findById(product.id()).get();
        assertEquals(Product.ApprovalStatus.APPROVED, p.getApprovalStatus(), "Product should remain APPROVED");
        assertTrue(p.isActive(), "Product should remain active");

        // Verify it is in the menu
        List<ProductResponse> active = productService.getAllActiveProducts();
        assertTrue(active.stream().anyMatch(pr -> pr.id().equals(product.id())), "Product should be in active list");
    }
}
