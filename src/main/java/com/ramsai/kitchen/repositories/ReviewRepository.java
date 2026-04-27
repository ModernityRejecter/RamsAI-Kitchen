package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r.product.id, AVG(r.rating) FROM Review r GROUP BY r.product.id")
    List<Object[]> findAverageRatingsPerProduct();
}
