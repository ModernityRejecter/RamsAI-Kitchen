package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.ProductPopularityResponse;
import com.ramsai.kitchen.models.dtos.ProductRatingResponse;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.repositories.OrderItemRepository;
import com.ramsai.kitchen.repositories.ProductRepository;
import com.ramsai.kitchen.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {

    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductPopularityResponse> getPopularityReport(Long categoryId) {
        List<Product> products = (categoryId != null) 
                ? productRepository.findAllByCategoryId(categoryId)
                : productRepository.findAll();

        Map<Long, Long> popularityMap = orderItemRepository.findPopularProducts().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return products.stream()
                .map(p -> new ProductPopularityResponse(
                        p.getId(),
                        p.getName(),
                        popularityMap.getOrDefault(p.getId(), 0L),
                        p.getCategory().getName()
                ))
                .sorted((a, b) -> b.totalQuantitySold().compareTo(a.totalQuantitySold()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductRatingResponse> getRatingsReport(Long categoryId) {
        List<Product> products = (categoryId != null)
                ? productRepository.findAllByCategoryId(categoryId)
                : productRepository.findAll();

        Map<Long, Double> ratingsMap = reviewRepository.findAverageRatingsPerProduct().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));

        return products.stream()
                .map(p -> new ProductRatingResponse(
                        p.getId(),
                        p.getName(),
                        ratingsMap.getOrDefault(p.getId(), 0.0),
                        p.getCategory().getName()
                ))
                .sorted((a, b) -> b.averageRating().compareTo(a.averageRating()))
                .collect(Collectors.toList());
    }
}
