package com.ramsai.kitchen.services;

import com.ramsai.kitchen.mappers.CategoryMapper;
import com.ramsai.kitchen.mappers.ProductMapper;
import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Product;
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
        log.info("Fetching all active approved products");
        return productRepository.findAllByIsActiveTrueAndApprovalStatus(Product.ApprovalStatus.APPROVED).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getDailyRecipes() {
        log.info("Fetching approved daily recipes");
        return productRepository.findAllByIsDailyRecipeTrueAndIsActiveTrueAndApprovalStatus(Product.ApprovalStatus.APPROVED).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getRecommendations() {
        log.info("Fetching approved recommendations");
        return productRepository.findTop6ByIsActiveTrueAndApprovalStatusOrderByAverageRatingDesc(Product.ApprovalStatus.APPROVED).stream()
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
        log.info("Fetching approved products for category {}", categoryId);
        return productRepository.findAllByCategoryIdAndApprovalStatus(categoryId, Product.ApprovalStatus.APPROVED).stream()
                .filter(Product::isActive)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}
