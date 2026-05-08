package com.cosmetics.ecommerce.service;

import java.util.List;

import com.cosmetics.ecommerce.dto.ProductReviewListResponseDTO;
import com.cosmetics.ecommerce.dto.ReviewReportDTO;
import com.cosmetics.ecommerce.dto.ReviewRequestDTO;
import com.cosmetics.ecommerce.dto.ReviewResponseDTO;

public interface ReviewService {
    ReviewResponseDTO createReview(Integer userId, Integer productId, ReviewRequestDTO request);
    ProductReviewListResponseDTO getProductReviews(Integer productId, Integer rating);
    List<ReviewResponseDTO> getAllReviewsForAdmin();
    ReviewResponseDTO updateReviewFlag(Integer reviewId, String flag);
    List<ReviewReportDTO> getReviewReport();
}
