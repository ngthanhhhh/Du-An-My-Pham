package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewReportDTO {
    private Integer productId;
    private String productName;
    private Long totalReviews;
    private Double averageRating;
    private Double satisfactionRate;
}
