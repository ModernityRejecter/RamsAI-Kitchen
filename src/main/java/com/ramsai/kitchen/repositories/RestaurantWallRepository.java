package com.ramsai.kitchen.repositories;

import com.ramsai.kitchen.models.entities.RestaurantWall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantWallRepository extends JpaRepository<RestaurantWall, Long> {
    Optional<RestaurantWall> findByXposAndYpos(Integer xpos, Integer ypos);
}
