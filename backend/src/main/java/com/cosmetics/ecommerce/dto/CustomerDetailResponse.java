package com.cosmetics.ecommerce.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record CustomerDetailResponse(
        Integer id,
        String name,
        String email,
        String phone,
        String address,
        LocalDateTime createdAt,
        Boolean isActive,
        List<OrderHistoryDTO> orderHistory
) {
    public record OrderHistoryDTO(
            Integer orderId,
            LocalDateTime createdAt,
            BigDecimal totalPrice,
            String status
    ){}
}
