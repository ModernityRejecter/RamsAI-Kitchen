package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {
    List<ProductIngredient> findAllByProductId(Long productId);
    List<ProductIngredient> findAllByIngredientId(Long ingredientId);
    boolean existsByProductIdAndIngredientId(Long productId, Long ingredientId);
    boolean existsByIngredientId(Long ingredientId);
}
