package com.cosmetics.ecommerce.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.dto.OrderDetailResponseDTO;
import com.cosmetics.ecommerce.dto.OrderListDTO;
import com.cosmetics.ecommerce.dto.OrderStatusUpdateResponseDTO;
import com.cosmetics.ecommerce.dto.UpdateOrderStatusRequestDTO;
import com.cosmetics.ecommerce.service.OrderService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý đơn hàng cho quyền Admin.
 */

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor //Tự động tạo Constructor để Inject các bean (orderService)
public class AdminOrderController {
    private final OrderService orderService;

    /**
     * Lấy danh sách đơn hàng có phân trang và lọc theo điều kiện
     * * @param status Trạng thái đơn hàng (PENDING,...) - không bắt buộc.
     * @param keyword Từ khóa tìm kiếm (theo tên khách hàng, mã đơn...) - không bắt buộc
     * @param page Số thứ tự trang muốn lấy (mặc định là 0).
     * @param size Số lượng bản ghi trên mỗi trang (mặc định là 10).
     * @return ResponseEntity chứa đối tượng Page các đơn hàng.
     */
    @GetMapping
    public ResponseEntity<Page<OrderListDTO>> getOrders(
        @RequestParam(required = false)  String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        //Gọi xuống tầng Service để xử lý logic lấy dữ liệu từ Database
        Page<OrderListDTO> result = orderService.getAdminOrders(status, keyword, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy thông tin chi tiết của 1 đơn hàng cụ thể theo ID
     * * @param id Mã định danh của đơn hàng
     * @return ResponseEntity chứa thông tin chi tiết đầy đủ của đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponseDTO> getOrderDetail(@PathVariable Integer id){
        //Tìm kiếm thông tin chi tiết đơn hàng cho Admin qua Service
        return ResponseEntity.ok(orderService.getOrderDetailForAdmin(id));
    }

    /**
     * Cập nhật trạng thái đơn hàng.
     *
     * API cho phép Admin thay đổi trạng thái của một đơn hàng cụ thể.
     * Backend sẽ:
     * - Kiểm tra tính hợp lệ của trạng thái mới
     * - Kiểm tra luồng chuyển trạng thái (không cho quay ngược)
     * - Kiểm tra thanh toán nếu chuyển sang COMPLETED
     * - Hoàn kho nếu chuyển sang CANCELLED
     *
     * @param id      ID của đơn hàng cần cập nhật
     * @param request Dữ liệu chứa trạng thái mới (status)
     * @return Thông tin trạng thái trước và sau khi cập nhật
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderStatusUpdateResponseDTO> updateOrderStatus(
            @PathVariable Integer id,
            @RequestBody UpdateOrderStatusRequestDTO request
    ) {
        OrderStatusUpdateResponseDTO result = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(result);
    }
}
