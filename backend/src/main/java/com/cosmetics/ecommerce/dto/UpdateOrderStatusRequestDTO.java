package com.cosmetics.ecommerce.dto;

import lombok.Data;

/**
 * DTO dùng để nhận request cập nhật trạng thái đơn hàng từ phía Admin.
 *
 * Áp dụng cho API:
 * - PUT /api/v1/admin/orders/{id}/status
 *
 * Nhiệm vụ:
 * - Mang trạng thái mới mà Admin muốn cập nhật
 * - Dữ liệu sẽ được validate và xử lý ở tầng Service
 *
 * Lưu ý:
 * - status không được null hoặc rỗng
 * - phải đúng format của OrderStatus enum
 * - phải hợp lệ theo luồng chuyển trạng thái của hệ thống
 */
@Data
public class UpdateOrderStatusRequestDTO {
    private String status;
}
