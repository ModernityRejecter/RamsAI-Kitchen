package com.ramsai.kitchen.services;

import com.ramsai.kitchen.mappers.CategoryMapper;
import com.ramsai.kitchen.mappers.ProductMapper;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Category;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.repositories.CategoryRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public List<ProductResponse> getAllActiveProducts() {
        log.info("Fetching all active products");
        return productRepository.findAllByIsActiveTrue().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getDailyRecipes() {
        log.info("Fetching daily recipes");
        return productRepository.findAllByIsDailyRecipeTrueAndIsActiveTrue().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getRecommendations() {
        log.info("Fetching recommendations");
        // For now, recommend top rated products. 
        // In the future, this can be personalized using OrderItemRepository.findPopularProducts() 
        // or user-specific history.
        return productRepository.findTop6ByIsActiveTrueOrderByAverageRatingDesc().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        log.info("Fetching products for category {}", categoryId);
        return productRepository.findAllByCategoryId(categoryId).stream()
                .filter(Product::isActive)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getAllProductsForManagement() {
        log.info("Fetching all products for manager");
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .category(category)
                .isSpecialOffer(Boolean.TRUE.equals(request.isSpecialOffer()))
                .isDailyRecipe(Boolean.TRUE.equals(request.isDailyRecipe()))
                .discountPrice(request.discountPrice())
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }
}
