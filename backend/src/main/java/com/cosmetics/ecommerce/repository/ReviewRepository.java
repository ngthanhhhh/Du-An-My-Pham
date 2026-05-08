package com.cosmetics.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cosmetics.ecommerce.dto.ReviewReportDTO;
import com.cosmetics.ecommerce.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer>{
    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Integer productId);
    List<Review> findByProduct_ProductIdAndRatingOrderByCreatedAtDesc(Integer productId, Integer rating);
    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT new com.cosmetics.ecommerce.dto.ReviewReportDTO(
                p.productId,
                p.name,
                COUNT(r.reviewId),
                AVG(r.rating),
                (AVG(r.rating) / 5.0) * 100
            )
            FROM Review r
            JOIN r.product p
            GROUP BY p.productId, p.name
            ORDER BY AVG(r.rating) DESC
            """)
    List<ReviewReportDTO> getReviewReport();
}
