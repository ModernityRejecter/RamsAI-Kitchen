package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.WallResponse;
import com.ramsai.kitchen.models.entities.RestaurantWall;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.repositories.RestaurantWallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WallService {

    private final RestaurantWallRepository wallRepository;
    private final RestaurantTableRepository tableRepository;

    @Transactional(readOnly = true)
    public List<WallResponse> getAllWalls() {
        return wallRepository.findAll().stream()
                .map(this::mapToWallResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WallResponse addWall(Integer xPos, Integer yPos) {
        if (wallRepository.findByXposAndYpos(xPos, yPos).isPresent() ||
                tableRepository.findByXposAndYpos(xPos, yPos).isPresent()) {
            throw new RuntimeException("Position already occupied");
        }
        RestaurantWall newWall = new RestaurantWall();
        newWall.setXpos(xPos);
        newWall.setYpos(yPos);
        RestaurantWall saved = wallRepository.save(newWall);
        return mapToWallResponse(saved);
    }

    @Transactional
    public WallResponse updateWallPosition(Long id, Integer xPos, Integer yPos) {
        RestaurantWall wall = wallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wall not found"));

        wallRepository.findByXposAndYpos(xPos, yPos).ifPresent(otherWall -> {
            if (!otherWall.getId().equals(id)) {
                throw new RuntimeException("Another wall already exists at this position");
            }
        });

        tableRepository.findByXposAndYpos(xPos, yPos).ifPresent(table -> {
            throw new RuntimeException("A table already exists at this position");
        });

        wall.setXpos(xPos);
        wall.setYpos(yPos);
        RestaurantWall savedWall = wallRepository.save(wall);
        return mapToWallResponse(savedWall);
    }

    @Transactional
    public void deleteWall(Long id) {
        wallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wall not found"));
        wallRepository.deleteById(id);
    }

    private WallResponse mapToWallResponse(RestaurantWall wall) {
        return new WallResponse(wall.getId(), wall.getXpos(), wall.getYpos());
    }
}
