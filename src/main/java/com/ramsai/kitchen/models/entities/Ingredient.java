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

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String unit; // e.g., g, ml, pcs

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double currentStock;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double minimumStockThreshold;

    @OneToMany(mappedBy = "ingredient")
    private Set<ProductIngredient> productIngredients;
}
