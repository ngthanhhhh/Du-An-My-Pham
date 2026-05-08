package com.cosmetics.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDTO {
    private Integer orderId;
    private String status;
    private BigDecimal totalPrice;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private String message;
}
