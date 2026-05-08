package com.cosmetics.ecommerce.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductReviewListResponseDTO {
    private Integer productId;
    private String productName;
    private Double averageRating;
    private Integer totalReviews;
    private List<ReviewResponseDTO> reviews;
}
