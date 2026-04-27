package com.ramsai.kitchen.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ingredient name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false)
    private String unit; // e.g., g, ml, pcs

    @NotNull(message = "Current stock is required")
    @PositiveOrZero(message = "Current stock cannot be negative")
    @Column(nullable = false)
    private Double currentStock;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double minimumStockThreshold;

    @OneToMany(mappedBy = "ingredient")
    private Set<ProductIngredient> productIngredients;
}
