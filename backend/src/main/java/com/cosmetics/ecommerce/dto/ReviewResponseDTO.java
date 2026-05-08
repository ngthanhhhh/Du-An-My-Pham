package com.cosmetics.ecommerce.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponseDTO {
    private Integer reviewId;
    private Integer productId;
    private String productName;
    private Integer userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private String adminFlag;
    private LocalDateTime createdAt;
}
