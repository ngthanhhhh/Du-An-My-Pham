package com.cosmetics.ecommerce.dto;

import lombok.Data;

@Data
public class ReviewRequestDTO {
    private Integer rating;
    private String comment;
}
