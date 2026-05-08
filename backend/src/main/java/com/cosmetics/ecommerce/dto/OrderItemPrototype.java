package com.cosmetics.ecommerce.dto;

import java.math.BigDecimal;

import com.cosmetics.ecommerce.common.Prototype;
import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.OrderItem;
import com.cosmetics.ecommerce.entity.Product;

import lombok.Getter;
import lombok.Setter;

/**
 * Concrete Prototype dùng để tạo "snapshot" dữ liệu sản phẩm tại thời điểm checkout.
 *
 * Ý tưởng chính:
 * - Không lấy trực tiếp dữ liệu từ Product khi tạo OrderItem
 * - Mà tạo một bản sao (clone) chứa dữ liệu tại thời điểm mua
 *
 * Lợi ích:
 * - Giữ nguyên tên sản phẩm, giá, số lượng tại thời điểm đặt hàng
 * - Không bị ảnh hưởng nếu Product thay đổi sau này (đổi giá, đổi tên, xóa sản phẩm)
 */
@Getter
@Setter
public class OrderItemPrototype implements Prototype<OrderItemPrototype>{
    private Integer productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;

    /**
     * Constructor dùng để tạo object mẫu (prototype) từ dữ liệu hiện tại.
     * 
     * Dùng trong bước checkout:
     * - Lấy dữ liệu từ Product + CartItem
     * - Tạo object ban đầu trước khi clone
     *
     * @param productId ID của sản phẩm
     * @param productName tên sản phẩm tại thời điểm mua
     * @param quantity số lượng khách hàng đặt
     * @param price giá sản phẩm tại thời điểm checkout
     */
    public OrderItemPrototype(Integer productId, String productName, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Copy constructor – phần quan trọng nhất của Prototype Pattern.
     * 
     * Nhiệm vụ:
     * - Sao chép toàn bộ dữ liệu từ object gốc (other)
     * - Tạo ra một object mới độc lập
     * 
     * Ý nghĩa:
     * - Object mới không phụ thuộc vào object cũ
     * - Đảm bảo dữ liệu snapshot không bị thay đổi ngoài ý muốn
     * 
     * @param other object mẫu cần sao chép
     */
    public OrderItemPrototype(OrderItemPrototype other) {
        this.productId = other.productId;
        this.productName = other.productName;
        this.quantity = other.quantity;
        this.price = other.price;
    }

    /**
     * Tạo prototype từ dữ liệu Product và số lượng trong giỏ hàng.
     * 
     * Mục đích:
     * - Giúp code ở Service gọn hơn, không cần new nhiều field
     * - Gom logic tạo prototype về một chỗ
     * 
     * Flow: Product + CartItem → Prototype → clone → OrderItem
     * 
     * Đây là bước CHUẨN BỊ trước khi clone:
     * Product → Prototype
     * @param product sản phẩm trong giỏ hàng
     * @param quantity số lượng khách đặt
     * @return object prototype chứa dữ liệu snapshot
     */
    public static OrderItemPrototype from(Product product, Integer quantity) {
        return new OrderItemPrototype(
            product.getProductId(),
            product.getName(),
            quantity,
            product.getPrice()
        );
    }

    /**
     * Phương thức clone() – nơi Client gọi để tạo bản sao từ object mẫu.
     * 
     * Khi gọi clone():
     * - Object tự tạo bản sao của chính nó thông qua copy constructor
     * - Client không cần biết cách copy bên trong
     * 
     * Đây là điểm thể hiện rõ Prototype Pattern:
     * tạo object từ object có sẵn, không phải từ constructor bên ngoài
     * 
     * Sau bước này:
     * - Snapshot hoàn toàn độc lập
     * - Không bị ảnh hưởng nếu Product thay đổi sau này
     */
    @Override
    public OrderItemPrototype clone() {
        return new OrderItemPrototype(this);
    }

    /**
     * Chuyển snapshot thành OrderItem entity để lưu vào database
     * Tại sao cần bước này:
     * - Prototype chỉ là object tạm (snapshot)
     * - Database chỉ lưu entity (OrderItem)
     * 
     * Nên cần convert:
     * snapshot → entity
     * 
     * Lợi ích:
     * - OrderItem giữ nguyên dữ liệu tại thời điểm mua
     * - Không bị ảnh hưởng bởi thay đổi của Product sau này
     * 
     * @param order đơn hàng chứa OrderItem
     * @param product entity Product (dùng để liên kết FK)
     * @return hoàn chỉnh để lưu database
     */
    public OrderItem toOrderItem(Order order, Product product) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setProductName(this.productName);
        orderItem.setQuantity(this.quantity);
        orderItem.setPrice(this.price);
        return orderItem;
    }


}
