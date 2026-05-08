package com.cosmetics.ecommerce.service;
import java.util.List;

import org.springframework.data.domain.Page;

import com.cosmetics.ecommerce.dto.OrderDetailResponseDTO;
import com.cosmetics.ecommerce.dto.OrderListDTO;
import com.cosmetics.ecommerce.dto.OrderRequestDTO;
import com.cosmetics.ecommerce.dto.OrderResponseDTO;
import com.cosmetics.ecommerce.dto.OrderStatusUpdateResponseDTO;
import com.cosmetics.ecommerce.dto.UpdateOrderStatusRequestDTO;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến đơn hàng.
 *
 * Bao gồm:
 * - Đặt hàng (checkout)
 * - Xem danh sách đơn hàng của khách hàng
 * - Admin xem danh sách đơn hàng
 * - Admin xem chi tiết đơn hàng
 * - Admin cập nhật trạng thái đơn hàng
 *
 * Ý nghĩa:
 * - Tách biệt tầng Service và Controller
 * - Giúp dễ mở rộng và test (có thể mock khi viết unit test)
 */
public interface OrderService {
    /**
     * Thực hiện chức năng đặt hàng cho người dùng.
     *
     * @param userId  ID của người dùng
     * @param request Thông tin đặt hàng (địa chỉ, phương thức thanh toán)
     * @return Thông tin đơn hàng sau khi đặt thành công
     */
    OrderResponseDTO placeOrder(Integer userId, OrderRequestDTO request);

    /**
     * Lấy danh sách đơn hàng của người dùng hiện tại.
     *
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng của user
     */
    List<OrderResponseDTO> getMyOrders(Integer userId);

    /**
     * Lấy danh sách đơn hàng cho Admin (có phân trang và filter).
     *
     * Hỗ trợ:
     * - Lọc theo trạng thái đơn hàng
     * - Tìm kiếm theo keyword (tên khách hàng, mã đơn...)
     * - Phân trang
     *
     * @param status  Trạng thái cần lọc (có thể null)
     * @param keyword Từ khóa tìm kiếm (có thể null)
     * @param page    Trang hiện tại
     * @param size    Số lượng bản ghi mỗi trang
     * @return Page chứa danh sách đơn hàng
     */
    Page<OrderListDTO> getAdminOrders(String status, String keyword, int page, int size);

    /**
     * Lấy thông tin chi tiết của một đơn hàng (dành cho Admin).
     *
     * Bao gồm:
     * - Thông tin khách hàng
     * - Danh sách sản phẩm
     * - Thông tin thanh toán
     *
     * @param orderId ID của đơn hàng
     * @return Thông tin chi tiết đơn hàng
     */
    OrderDetailResponseDTO getOrderDetailForAdmin(Integer orderId);

    /**
     * Cập nhật trạng thái của đơn hàng (dành cho Admin).
     *
     * Quy trình:
     * - Kiểm tra đơn hàng tồn tại
     * - Validate trạng thái mới
     * - Kiểm tra luồng chuyển trạng thái hợp lệ
     * - Nếu COMPLETED -> kiểm tra thanh toán thành công
     * - Nếu CANCELLED -> hoàn trả sản phẩm về kho
     *
     * @param orderId ID của đơn hàng
     * @param request Trạng thái mới cần cập nhật
     * @return Thông tin kết quả cập nhật trạng thái
     */
    OrderStatusUpdateResponseDTO updateOrderStatus(Integer orderId, UpdateOrderStatusRequestDTO request);

    OrderDetailResponseDTO getOrderDetailForCustomer(Integer userId, Integer orderId);
}
