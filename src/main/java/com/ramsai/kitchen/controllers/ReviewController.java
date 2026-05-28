package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.ReviewRequest;
import com.ramsai.kitchen.models.dtos.ReviewResponse;
import com.ramsai.kitchen.models.entities.User;
import com.ramsai.kitchen.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse data = reviewService.createReview(user.getId(), request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Map<String, Object>> getProductReviews(@PathVariable Long productId) {
        List<ReviewResponse> data = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<Map<String, Object>> getMyReviews(@AuthenticationPrincipal User user) {
        List<ReviewResponse> data = reviewService.getReviewsByUser(user.getId());
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse data = reviewService.updateReview(reviewId, user.getId(), request);
        return ResponseEntity.ok(Map.of(
                "data", data,
                "message", "Success"
        ));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user) {
        reviewService.deleteReview(reviewId, user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Success"
        ));
    }
}
