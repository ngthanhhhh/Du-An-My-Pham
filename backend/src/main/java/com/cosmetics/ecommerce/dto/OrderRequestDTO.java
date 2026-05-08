package com.cosmetics.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderRequestDTO {

    @NotBlank(message = "Họ tên không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại không hợp lệ, vui lòng nhập đủ 10 chữ số!") //chuoi fai dung 10 chu so
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống!")
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được để trống!")
    private String paymentMethod;
}
