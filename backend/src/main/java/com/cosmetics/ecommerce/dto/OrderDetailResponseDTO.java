package com.cosmetics.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailResponseDTO {
    private Integer orderId;

    private String customerName;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String status;
    private LocalDateTime createdAt;

    private Boolean hasPayment;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String transactionNo;
    private LocalDateTime paymentDate;
    private BigDecimal totalPrice;
    private String paymentMessage; //Cho trường hợp chưa có t/tin thanh toán

    private List<OrderItemDTO> items;
}
