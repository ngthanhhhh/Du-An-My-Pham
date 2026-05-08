package com.cosmetics.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.OrderItem;
import com.cosmetics.ecommerce.enums.OrderStatus;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer>{
    List<OrderItem> findByOrder(Order order); //demo thoi

    boolean existsByOrder_User_UserIdAndProduct_ProductIdAndOrder_Status(
        Integer userId,
        Integer productId,
        OrderStatus status
    );
}
