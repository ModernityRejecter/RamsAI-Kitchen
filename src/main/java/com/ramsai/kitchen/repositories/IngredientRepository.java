package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Ingredient> findByNameIgnoreCase(String name);
}
