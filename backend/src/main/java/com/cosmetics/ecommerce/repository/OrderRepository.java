package com.cosmetics.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>{
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    //Tìm kiếm theo tên/sđt; lọc theo trạng thái
    @Query("SELECT o FROM Order o WHERE " +
        "(:status IS NULL OR o.status = :status) AND " +
        "(:keyword IS NULL OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))")
    Page<Order> searchAdminOrders(@Param("status") OrderStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    //Lấy danh sách đơn hàng của một người dùng
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    //Lấy danh sách đơn hàng theo UserId để phục vụ CustomerService
    List<Order> findByUserUserId(Integer userId);

    //Tính tổng doanh thu từ các đơn hàng đã giao hàng thành công (COMPLETED)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = com.cosmetics.ecommerce.enums.OrderStatus.COMPLETED")
    Double calculateTotalRevenue();

    //Đếm tổng số đơn hàng strong hệ thống
    @Query("SELECT COUNT(o) FROM Order o")
    Long countTotalOrders();

    //Đếm số đơn hàng theo trạng thái (ví dụ: PENDING, DELIVERD, CANCELLED)
    //giúp admin biết có bao nhiêu đơn đang xử lý
    Long countByStatus(OrderStatus status);


}