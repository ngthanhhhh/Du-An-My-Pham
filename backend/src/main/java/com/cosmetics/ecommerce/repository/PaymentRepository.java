package com.cosmetics.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.cosmetics.ecommerce.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    /**
     * Tìm thông tin thanh toán theo ID của đơn hàng.
     *
     * @param orderId ID của đơn hàng
     * @return Optional chứa Payment nếu tồn tại, ngược lại rỗng
     */
    Optional<Payment> findByOrder_OrderId(Integer orderId);

    /**
     * Tìm thông tin thanh toán theo mã giao dịch.
     *
     * @param transactionNo mã giao dịch (transaction number)
     * @return Optional chứa Payment nếu tồn tại, ngược lại trả về Optional rỗng
     */
    Optional<Payment> findByTransactionNo(String transactionNo);
}