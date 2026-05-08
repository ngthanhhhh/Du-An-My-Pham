package com.cosmetics.ecommerce.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private Integer orderItemId;
    private Integer productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price; //Giá tại lúc mua
    private BigDecimal subTotal; //thành tiền
    private String note; //VD: Sản phẩm hiện ngừng kinh doanh cho FE biết
    private Boolean discontinued; //Sản phẩm bị xóa khỏi hệ thống/ko còn liên kết product -> true
}
