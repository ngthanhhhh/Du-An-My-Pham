package com.cosmetics.ecommerce.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để trả về kết quả sau khi cập nhật trạng thái đơn hàng.
 *
 * Áp dụng cho API:
 * - PUT /api/v1/admin/orders/{id}/status
 *
 * Nhiệm vụ:
 * - Cung cấp thông tin trạng thái trước và sau khi cập nhật
 * - Trả về thông báo kết quả cho frontend
 * - Hỗ trợ frontend hiển thị các trạng thái hợp lệ tiếp theo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateResponseDTO {
    private Integer orderId;
    private String oldStatus;
    private String newStatus;
    private String message;

    /**
     * Danh sách các trạng thái hợp lệ tiếp theo
     * dựa trên trạng thái hiện tại của đơn hàng.
     */
    private List<String> availableNextStatus;
}
