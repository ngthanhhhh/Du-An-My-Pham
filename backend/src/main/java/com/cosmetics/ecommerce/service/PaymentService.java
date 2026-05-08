package com.cosmetics.ecommerce.service;

import java.util.Map;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến thanh toán VNPay.
 *
 * Bao gồm:
 * - Tạo URL thanh toán VNPay cho đơn hàng
 * - Xử lý phản hồi (callback) từ VNPay sau khi người dùng thanh toán
 *
 */
public interface PaymentService {
    /**
     * Tạo URL thanh toán VNPay cho một đơn hàng.
     *
     * Quy trình:
     * - Validate đơn hàng và thông tin thanh toán
     * - Tạo mã giao dịch (txnRef)
     * - Build tham số gửi VNPay
     * - Ký dữ liệu bằng HMAC SHA512
     * - Trả về URL hoàn chỉnh để frontend redirect
     *
     * @param orderId ID của đơn hàng
     * @param ipAddress Địa chỉ IP của người dùng thực hiện thanh toán
     * @return URL thanh toán VNPay
     */
    String createVnPayPaymentUrl(Integer userId, Integer orderId, String ipAddress);

    /**
     * Xử lý phản hồi trả về từ VNPay sau khi thanh toán.
     *
     * Quy trình:
     * - Nhận toàn bộ params VNPay gửi về
     * - Xác thực chữ ký (hash) để đảm bảo dữ liệu không bị sửa
     * - Lấy mã giao dịch để tìm Payment
     * - Cập nhật trạng thái thanh toán (SUCCESS / FAILED)
     * - Cập nhật trạng thái đơn hàng tương ứng
     *
     * @param params Map chứa toàn bộ tham số VNPay gửi về
     * @return Thông báo kết quả xử lý thanh toán
     */
    String handleVnPayReturn(Map<String, String> params);
}
