package com.cosmetics.ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.dto.ProductReviewListResponseDTO;
import com.cosmetics.ecommerce.dto.ReviewRequestDTO;
import com.cosmetics.ecommerce.dto.ReviewResponseDTO;
import com.cosmetics.ecommerce.security.CurrentUserProvider;
import com.cosmetics.ecommerce.service.ReviewService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ReviewResponseDTO> createReview(
        Authentication authentication,
        @PathVariable Integer productId,
        @RequestBody ReviewRequestDTO request
    ){
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        ReviewResponseDTO response = reviewService.createReview(userId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ProductReviewListResponseDTO> getProductReviews(
        @PathVariable Integer productId,
        @RequestParam(required = false) Integer rating
    ) {
        ProductReviewListResponseDTO response = reviewService.getProductReviews(productId, rating);
        return ResponseEntity.ok(response);
    }
}
