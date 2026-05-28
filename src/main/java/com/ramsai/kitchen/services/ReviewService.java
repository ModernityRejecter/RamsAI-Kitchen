package com.ramsai.kitchen.services;

import com.ramsai.kitchen.enums.OrderStatus;
import com.ramsai.kitchen.exceptions.ResourceNotFoundException;
import com.ramsai.kitchen.mappers.ReviewMapper;
import com.ramsai.kitchen.models.dtos.ReviewRequest;
import com.ramsai.kitchen.models.dtos.ReviewResponse;
import com.ramsai.kitchen.models.entities.Order;
import com.ramsai.kitchen.models.entities.OrderItem;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.models.entities.Review;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.repositories.ReviewRepository;
import com.ramsai.kitchen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(Long customerId, ReviewRequest request) {
        log.info("Creating review for product {} by user {}", request.productId(), customerId);

        if (reviewRepository.existsByProductIdAndCustomerId(request.productId(), customerId)) {
            throw new IllegalArgumentException("You have already reviewed this product.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if the user has a completed order with this product
        List<Order> customerOrders = orderRepository.findCustomerOrders(customerId, OrderStatus.DRAFT);
        boolean hasOrdered = customerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.SERVED)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(request.productId()));

        if (!hasOrdered) {
            throw new IllegalArgumentException("You can only review products you have ordered and consumed (order must be SERVED).");
        }

        Review review = Review.builder()
                .product(product)
                .customer(customer)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        Review savedReview = reviewRepository.save(review);
        updateProductAverageRating(product);

        return reviewMapper.toResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long customerId, ReviewRequest request) {
        log.info("Updating review {} by user {}", reviewId, customerId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("You can only edit your own reviews.");
        }

        review.setRating(request.rating());
        review.setComment(request.comment());

        Review updatedReview = reviewRepository.save(review);
        updateProductAverageRating(review.getProduct());

        return reviewMapper.toResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long customerId) {
        log.info("Deleting review {} by user {}", reviewId, customerId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("You can only delete your own reviews.");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        
        // Ensure delete is flushed before computing the new average rating
        reviewRepository.flush();
        updateProductAverageRating(product);
    }

    private void updateProductAverageRating(Product product) {
        Double avgRating = reviewRepository.getAverageRatingForProduct(product.getId());
        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        productRepository.save(product);
    }
}
