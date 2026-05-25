package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByXposAndYpos(Integer xpos, Integer ypos);
}
