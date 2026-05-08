package com.cosmetics.ecommerce.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.security.CurrentUserProvider;
import com.cosmetics.ecommerce.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Controller xử lý các API liên quan đến thanh toán VNPay.
 *
 * Bao gồm:
 * - Tạo link thanh toán cho đơn hàng
 * - Nhận kết quả thanh toán từ VNPay (callback/redirect)
 *
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * API tạo link thanh toán VNPay cho một đơn hàng.
     *
     * Flow:
     * - Nhận orderId từ frontend
     * - Lấy IP của người dùng
     * - Gọi service để tạo URL thanh toán
     * - Trả về URL để frontend redirect sang VNPay
     *
     * @param orderId ID của đơn hàng cần thanh toán
     * @param request HttpServletRequest để lấy địa chỉ IP
     * @return JSON chứa URL thanh toán
     */
    @PostMapping("/vnpay/{orderId}")
    public ResponseEntity<Map<String, String>> createVnPayPayment(
        Authentication authentication,
        @PathVariable Integer orderId,
        HttpServletRequest request
    ){
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        // Lấy IP của client (VNPay yêu cầu)
        String ipAddress = request.getRemoteAddr();

        // Gọi service tạo URL thanh toán
        String paymentUrl = paymentService.createVnPayPaymentUrl(userId, orderId, ipAddress);

        // Trả về cho frontend dưới dạng JSON
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    /**
     * API nhận phản hồi từ VNPay sau khi người dùng thanh toán.
     *
     * Flow:
     * - VNPay redirect về endpoint này kèm theo params
     * - Truyền toàn bộ params xuống service để xử lý
     * - Trả kết quả về frontend (thành công / thất bại)
     *
     * @param params Toàn bộ query params VNPay gửi về
     * @return JSON chứa thông báo kết quả thanh toán
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, String>> vnpayReturn(
        @RequestParam Map<String, String> params
    ){
        // Gọi service xử lý callback từ VNPay
        String message = paymentService.handleVnPayReturn(params);

        // Trả kết quả về frontend
        return ResponseEntity.ok(Map.of("message", message));
    }
}
