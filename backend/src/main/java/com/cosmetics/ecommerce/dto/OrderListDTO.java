package com.cosmetics.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderListDTO {
    private Integer orderId;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
}