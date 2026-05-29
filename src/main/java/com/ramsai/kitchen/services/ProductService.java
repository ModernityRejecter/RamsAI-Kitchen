package com.ramsai.kitchen.services;

import com.ramsai.kitchen.mappers.CategoryMapper;
import com.ramsai.kitchen.mappers.ProductMapper;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.dtos.ProductUpdateRequest;
import com.ramsai.kitchen.models.dtos.CategoryResponse;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Category;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.repositories.CategoryRepository;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public List<ProductResponse> getAllActiveProducts() {
        log.info("Fetching all approved products");
        return productRepository.findAllByApprovalStatus(Product.ApprovalStatus.APPROVED).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getDailyRecipes() {
        log.info("Fetching approved daily recipes");
        return productRepository.findAllByIsDailyRecipeTrueAndApprovalStatus(Product.ApprovalStatus.APPROVED).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getRecommendations() {
        log.info("Fetching approved recommendations");
        return productRepository.findTop6ByApprovalStatusOrderByAverageRatingDesc(Product.ApprovalStatus.APPROVED).stream()
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
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getAllProductsForManagement() {
        log.info("Fetching all products for management");
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        // Always set to PENDING for new products (especially those created via AI/Chef)
        // unless a different policy is requested. The user specifically asked for PENDING.
        Product.ApprovalStatus status = Product.ApprovalStatus.PENDING;

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .category(category)
                .isSpecialOffer(Boolean.TRUE.equals(request.isSpecialOffer()))
                .isDailyRecipe(Boolean.TRUE.equals(request.isDailyRecipe()))
                .discountPrice(request.discountPrice())
                .approvalStatus(status)
                .isActive(false) // Inactive until approved
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setCategory(category);
        product.setSpecialOffer(Boolean.TRUE.equals(request.isSpecialOffer()));
        product.setDailyRecipe(Boolean.TRUE.equals(request.isDailyRecipe()));
        product.setDiscountPrice(request.discountPrice());

        User currentUser = (User) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentUser.getRole() == com.ramsai.kitchen.models.entities.User.UserRole.CHEF) {
            // Reset to pending and inactive
            product.setApprovalStatus(Product.ApprovalStatus.PENDING);
            product.setActive(false);
            product.setRejectionFeedback(null);
        }

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse approveProduct(Long id) {
        log.info("Approving product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setApprovalStatus(Product.ApprovalStatus.APPROVED);
        product.setActive(true);
        product.setRejectionFeedback(null);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse rejectProduct(Long id, String feedback) {
        log.info("Rejecting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setApprovalStatus(Product.ApprovalStatus.REJECTED);
        product.setActive(false);
        product.setRejectionFeedback(feedback);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProductActiveStatus(Long id, boolean isActive) {
        log.info("Updating active status for product {}: {}", id, isActive);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (isActive && product.getApprovalStatus() == Product.ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot activate a rejected product. It must be approved first.");
        }
        product.setActive(isActive);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        if (orderItemRepository.existsByProductId(id)) {
            throw new RuntimeException("Cannot delete product used in orders.");
        }
        
        productRepository.delete(product);
    }
}
