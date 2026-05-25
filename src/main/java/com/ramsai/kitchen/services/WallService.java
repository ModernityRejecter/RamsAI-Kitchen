package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.dtos.WallResponse;
import com.ramsai.kitchen.models.entities.RestaurantWall;
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

    @Transactional(readOnly = true)
    public List<WallResponse> getAllWalls() {
        return wallRepository.findAll().stream()
                .map(this::mapToWallResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WallResponse addWall() {
        RestaurantWall newWall = new RestaurantWall();
        
        int x = 0, y = 0;
        while (wallRepository.findByXposAndYpos(x, y).isPresent()) {
            x++;
            if (x > 14) {
                x = 0;
                y++;
            }
        }
        newWall.setXpos(x);
        newWall.setYpos(y);

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

        wall.setXpos(xPos);
        wall.setYpos(yPos);
        RestaurantWall savedWall = wallRepository.save(wall);
        return mapToWallResponse(savedWall);
    }

    private WallResponse mapToWallResponse(RestaurantWall wall) {
        return new WallResponse(wall.getId(), wall.getXpos(), wall.getYpos());
    }
}
