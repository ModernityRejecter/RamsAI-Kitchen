package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByCategoryId(Long categoryId);
    List<Product> findAllByIsActiveTrue();
    List<Product> findAllByIsDailyRecipeTrueAndIsActiveTrue();
    List<Product> findTop6ByIsActiveTrueOrderByAverageRatingDesc();

    List<Product> findAllByCategoryIdAndApprovalStatus(Long categoryId, Product.ApprovalStatus status);
    List<Product> findAllByIsActiveTrueAndApprovalStatus(Product.ApprovalStatus status);
    List<Product> findAllByIsDailyRecipeTrueAndIsActiveTrueAndApprovalStatus(Product.ApprovalStatus status);
    List<Product> findTop6ByIsActiveTrueAndApprovalStatusOrderByAverageRatingDesc(Product.ApprovalStatus status);
}
