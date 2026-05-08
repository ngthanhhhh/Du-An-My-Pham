package com.cosmetics.ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.dto.ReviewReportDTO;
import com.cosmetics.ecommerce.dto.ReviewResponseDTO;
import com.cosmetics.ecommerce.dto.UpdateReviewFlagRequestDTO;
import com.cosmetics.ecommerce.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponseDTO>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviewsForAdmin());
    }

    @PutMapping("/{reviewId}/flag")
    public ResponseEntity<ReviewResponseDTO> updateReviewFlag(
        @PathVariable Integer reviewId,
        @RequestBody UpdateReviewFlagRequestDTO request
    ) {
        return ResponseEntity.ok(reviewService.updateReviewFlag(reviewId, request.getFlag()));
    }

    @GetMapping("/report")
    public ResponseEntity<List<ReviewReportDTO>> getReviewReport(){
        return ResponseEntity.ok(reviewService.getReviewReport());
    }
}
