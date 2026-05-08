package com.cosmetics.ecommerce.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.dto.ProductReviewListResponseDTO;
import com.cosmetics.ecommerce.dto.ReviewReportDTO;
import com.cosmetics.ecommerce.dto.ReviewRequestDTO;
import com.cosmetics.ecommerce.dto.ReviewResponseDTO;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.entity.Review;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.enums.OrderStatus;
import com.cosmetics.ecommerce.enums.ReviewAdminFlag;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.OrderItemRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.repository.ReviewRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.ReviewService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service triển khai các nghiệp vụ liên quan đến đánh giá sản phẩm (Review).
 *
 * Bao gồm:
 * - Tạo đánh giá cho sản phẩm
 * - Xem danh sách đánh giá của sản phẩm (có lọc theo số sao)
 * - Admin xem toàn bộ đánh giá
 * - Admin cập nhật trạng thái kiểm duyệt đánh giá
 * - Lấy báo cáo tổng hợp đánh giá
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Tạo đánh giá mới cho sản phẩm.
     *
     * Quy trình:
     * - Validate request (rating hợp lệ)
     * - Kiểm tra user tồn tại
     * - Kiểm tra sản phẩm tồn tại
     * - Kiểm tra user đã mua sản phẩm chưa (verified purchase)
     * - Tạo Review và gán các thông tin cần thiết
     * - Lưu vào database
     * - Trả về DTO cho frontend
     *
     * @param userId ID người dùng
     * @param productId ID sản phẩm
     * @param request Nội dung đánh giá
     * @return Thông tin đánh giá vừa tạo
     */
    @Override
    @Transactional
    public ReviewResponseDTO createReview(Integer userId, Integer productId, ReviewRequestDTO request) {
        validateUserId(userId);
        validateProductId(productId);
        validateReviewRequest(request);

        User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại!"));
        Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại!"));

        // Kiểm tra user đã mua sản phẩm này và đơn đã COMPLETED hay chưa
        boolean isVerifiedPurchase = orderItemRepository.existsByOrder_User_UserIdAndProduct_ProductIdAndOrder_Status(
            userId, productId, OrderStatus.COMPLETED
        );

        // Tạo review mới
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
        review.setIsVerifiedPurchase(isVerifiedPurchase);

        // Mặc định review ở trạng thái bình thường (chưa bị flag)
        review.setAdminFlag(ReviewAdminFlag.NORMAL);

        // Đánh dấu có phải người mua thật hay không
        Review savedReview = reviewRepository.save(review);

        return mapToResponse(savedReview);
    }

    /**
     * Lấy danh sách đánh giá của một sản phẩm (có thể lọc theo số sao).
     *
     * Quy trình:
     * - Kiểm tra sản phẩm tồn tại
     * - Validate rating filter (nếu có)
     * - Lấy toàn bộ review để tính điểm trung bình
     * - Nếu có filter → chỉ lấy review theo số sao
     * - Tính average rating
     * - Map sang DTO
     *
     * @param productId ID sản phẩm
     * @param rating Số sao cần lọc (có thể null)
     * @return Thông tin tổng hợp đánh giá sản phẩm
     */
    @Override
    @Transactional(readOnly = true)
    public ProductReviewListResponseDTO getProductReviews(Integer productId, Integer rating) {
        validateProductId(productId);
        Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại!"));

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException("Số sao lọc phải nằm trong khoảng từ 1 đến 5");
        }

        // Lấy tất cả review để tính điểm trung bình đúng theo toàn bộ sản phẩm 
        // (không bị ảnh hưởng bởi filter)
        List<Review> allReviews = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId);

        // Danh sách hiển thị có thể bị filter theo rating
        List<Review> displayedReviews = rating == null
                ? allReviews
                : reviewRepository.findByProduct_ProductIdAndRatingOrderByCreatedAtDesc(productId, rating);

        // Tính điểm trung bình
        double averageRating = allReviews.isEmpty()
                ? 0.0
                : allReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);

        // Map sang DTO
        List<ReviewResponseDTO> reviewResponses = displayedReviews.stream()
            .map(this::mapToResponse)
            .toList();

        return ProductReviewListResponseDTO.builder()
            .productId(product.getProductId())
            .productName(product.getName())
            .averageRating(averageRating)
            .totalReviews(allReviews.size())
            .reviews(reviewResponses)
            .build();
    }

    /**
     * Lấy toàn bộ đánh giá (dành cho Admin).
     *
     * @return Danh sách tất cả review
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getAllReviewsForAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }
    /**
     * Cập nhật trạng thái kiểm duyệt của một đánh giá (Admin).
     *
     * Quy trình:
     * - Validate input flag
     * - Tìm review
     * - Parse flag sang enum
     * - Cập nhật trạng thái
     * - Lưu và trả về DTO
     *
     * @param reviewId ID đánh giá
     * @param flag Trạng thái mới (NORMAL, NEGATIVE_FEEDBACK, ATTENTION_NEEDED)
     * @return Review sau khi cập nhật
     */
    @Override
    @Transactional
    public ReviewResponseDTO updateReviewFlag(Integer reviewId, String flag) {
        validateReviewId(reviewId);

        if (flag == null || flag.trim().isEmpty()) {
            throw new BadRequestException("Trạng thái đánh giá không được để trống");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại"));

        ReviewAdminFlag newFlag;

        try {
            newFlag = ReviewAdminFlag.valueOf(flag.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái đánh giá không hợp lệ");
        }

        if (review.getAdminFlag() == newFlag) {
            throw new BadRequestException("Đánh giá đang ở trạng thái này.");
        }

        review.setAdminFlag(newFlag);

        Review updatedReview = reviewRepository.save(review);
        return mapToResponse(updatedReview);
    }

    /**
     * Lấy báo cáo tổng hợp đánh giá (thống kê).
     *
     * @return Danh sách report (custom query)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewReportDTO> getReviewReport() {
        return reviewRepository.getReviewReport();
    }

    /**
     * Validate request tạo review.
     *
     * Điều kiện:
     * - Request không null
     * - Rating không null
     * - Rating nằm trong khoảng 1 → 5
     */
    private void validateReviewRequest(ReviewRequestDTO request){
        if (request == null) {
            throw new BadRequestException("Dữ liệu đánh giá không hợp lệ!");
        }

        if (request.getRating() == null) {
            throw new BadRequestException("Số sao đánh giá không được để trống!");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Số sao đánh giá phải nằm trong khoảng từ 1 đến 5");
        }
    }

    /**
     * Mapping Review entity sang DTO trả về cho client.
     */
    private ReviewResponseDTO mapToResponse(Review review) {
        Product product = review.getProduct();
        User user = review.getUser();

        return ReviewResponseDTO.builder()
                .reviewId(review.getReviewId())
                .productId(product != null ? product.getProductId() : null)
                .productName(product != null ? product.getName() : "Sản phẩm không còn tồn tại!")
                .userId(user != null ? user.getUserId() : null)
                .userName(user != null ? user.getName() : "Người dùng không còn tồn tại!")
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .adminFlag(review.getAdminFlag() != null ? review.getAdminFlag().name() : ReviewAdminFlag.NORMAL.name())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new BadRequestException("Người dùng không hợp lệ!");
        }
    }

    private void validateProductId(Integer productId) {
        if (productId == null) {
            throw new BadRequestException("Mã sản phẩm không hợp lệ!");
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }
        return comment.trim();
    }

    private void validateReviewId(Integer reviewId) {
        if (reviewId == null) {
            throw new BadRequestException("Mã đánh giá không hợp lệ!");
        }
    }
}
