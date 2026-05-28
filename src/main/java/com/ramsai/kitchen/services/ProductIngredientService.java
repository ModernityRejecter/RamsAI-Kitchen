package com.ramsai.kitchen.services;

import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.models.dtos.ProductIngredientResponse;
import com.ramsai.kitchen.models.dtos.ProductIngredientUpsertRequest;
import com.ramsai.kitchen.models.entities.Ingredient;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.ProductIngredient;
import com.ramsai.kitchen.repositories.IngredientRepository;
import com.ramsai.kitchen.repositories.ProductIngredientRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductIngredientService {

    private final ProductIngredientRepository productIngredientRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public List<ProductIngredientResponse> getByProductId(Long productId) {
        ensureProductExists(productId);
        return productIngredientRepository.findAllByProductId(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductIngredientResponse create(Long productId, ProductIngredientUpsertRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        Ingredient ingredient = ingredientRepository.findById(request.ingredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + request.ingredientId()));

        if (productIngredientRepository.existsByProductIdAndIngredientId(productId, request.ingredientId())) {
            throw new RuntimeException("Ingredient is already assigned to this product.");
        }

        product.setApprovalStatus(Product.ApprovalStatus.PENDING);
        product.setActive(false);
        product.setRejectionFeedback(null);
        productRepository.save(product);

        ProductIngredient productIngredient = ProductIngredient.builder()
                .product(product)
                .ingredient(ingredient)
                .quantityRequired(request.quantityRequired())
                .build();
        return toResponse(productIngredientRepository.save(productIngredient));
    }

    @Transactional
    public ProductIngredientResponse update(Long id, ProductIngredientUpsertRequest request) {
        ProductIngredient productIngredient = productIngredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product ingredient not found with id: " + id));

        Ingredient ingredient = ingredientRepository.findById(request.ingredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + request.ingredientId()));

        Long productId = productIngredient.getProduct().getId();
        if (!productIngredient.getIngredient().getId().equals(request.ingredientId())
                && productIngredientRepository.existsByProductIdAndIngredientId(productId, request.ingredientId())) {
            throw new RuntimeException("Ingredient is already assigned to this product.");
        }

        productIngredient.setIngredient(ingredient);
        productIngredient.setQuantityRequired(request.quantityRequired());
        return toResponse(productIngredientRepository.save(productIngredient));
    }

    @Transactional
    public void delete(Long id) {
        ProductIngredient productIngredient = productIngredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product ingredient not found with id: " + id));

        Product product = productIngredient.getProduct();
        product.setApprovalStatus(Product.ApprovalStatus.PENDING);
        product.setActive(false);
        product.setRejectionFeedback(null);
        productRepository.save(product);

        productIngredientRepository.delete(productIngredient);
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
    }

    private ProductIngredientResponse toResponse(ProductIngredient productIngredient) {
        return new ProductIngredientResponse(
                productIngredient.getId(),
                productIngredient.getProduct().getId(),
                productIngredient.getIngredient().getId(),
                productIngredient.getIngredient().getName(),
                productIngredient.getIngredient().getUnit(),
                productIngredient.getQuantityRequired()
        );
    }
}
