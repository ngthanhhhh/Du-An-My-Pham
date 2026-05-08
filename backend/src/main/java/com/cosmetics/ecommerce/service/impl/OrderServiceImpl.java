package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.cosmetics.ecommerce.exception.BadRequestException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmetics.ecommerce.dto.OrderDetailResponseDTO;
import com.cosmetics.ecommerce.dto.OrderItemDTO;
import com.cosmetics.ecommerce.dto.OrderItemPrototype;
import com.cosmetics.ecommerce.dto.OrderListDTO;
import com.cosmetics.ecommerce.dto.OrderRequestDTO;
import com.cosmetics.ecommerce.dto.OrderResponseDTO;
import com.cosmetics.ecommerce.dto.OrderStatusUpdateResponseDTO;
import com.cosmetics.ecommerce.dto.UpdateOrderStatusRequestDTO;
import com.cosmetics.ecommerce.entity.*;
import com.cosmetics.ecommerce.enums.OrderStatus;
import com.cosmetics.ecommerce.enums.PaymentMethod;
import com.cosmetics.ecommerce.enums.PaymentStatus;
import com.cosmetics.ecommerce.enums.ProductStatus;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.CartItemRepository;
import com.cosmetics.ecommerce.repository.CartRepository;
import com.cosmetics.ecommerce.repository.OrderItemRepository;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.repository.PaymentRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.OrderService;

import java.util.Collections;
import lombok.RequiredArgsConstructor;

/**
 * Service triển khai các nghiệp vụ liên quan đến đơn hàng.
 * Bao gồm:
 * - Đặt hàng (checkout)
 * - Xem lịch sử đơn hàng của khách hàng
 * - Admin xem danh sách đơn hàng
 * - Admin xem chi tiết đơn hàng
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{

    private static final String PRODUCT_DISCONTINUED_NOTE = "Sản phẩm này hiện đã ngừng kinh doanh";
    private static final String NO_PAYMENT_MESSAGE = "Chưa có thông tin thanh toán";

    /**
     * Quy tắc chuyển đổi trạng thái đơn hàng.
     *
     * Key: trạng thái hiện tại
     * Value: danh sách trạng thái được phép chuyển tới
     *
     * Dùng để đảm bảo:
     * - Không cho quay ngược quy trình
     * - Không cho chuyển trạng thái sai logic nghiệp vụ
     */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        OrderStatus.PENDING, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING, Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
        OrderStatus.SHIPPING, Set.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, Set.of(OrderStatus.COMPLETED),
        OrderStatus.COMPLETED, Collections.emptySet(),
        OrderStatus.CANCELLED, Collections.emptySet()
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Thực hiện chức năng đặt hàng cho người dùng.
     *
     * Quy trình xử lý:
     * - Kiểm tra người dùng tồn tại
     * - Lấy giỏ hàng hiện tại
     * - Kiểm tra số lượng tồn kho của từng sản phẩm
     * - Tạo đơn hàng và chi tiết đơn hàng
     * - Tạo thông tin thanh toán
     * - Xóa giỏ hàng sau khi đặt thành công
     *
     * @param userId  ID của người dùng đang đặt hàng
     * @param request Thông tin người nhận và phương thức thanh toán
     * @return Thông tin đơn hàng sau khi đặt thành công
     */
    @Override
    @Transactional
    public OrderResponseDTO placeOrder(Integer userId, OrderRequestDTO request) {

        validateUserId(userId);
        validateOrderRequest(request);

        //1. Kiểm tra sự tồn tại của User
        User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));

        //2. Lấy giỏ hàng của User và kiểm tra tính hợp lệ
        Cart cart = cartRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng của bạn hiện đang trống!"));

        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống, không thể đặt hàng!");
        }

        //3. Khởi tạo đối tượng Đơn hàng (Order) với trạng thái mặc định là PENDING
        Order order = new Order();
        order.setUser(user);
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.ZERO); //Tạm thời gán bằng 0, cộng dồn sau

        Order savedOrder = orderRepository.save(order);
        BigDecimal totalAmount = BigDecimal.ZERO;

        //4. Xử lý từng sản phẩm trong giỏ: Kiểm tra kho, trừ kho và tạo chi tiết đơn hàng (OrderItem)
        for (CartItem item : cartItems) {
            //Sử dụng Pessimistic Lock (findByIdWithLock) để khóa row sản phẩm trong csdl.
            //-> Ngăn tình trạng "Race Condition" khi nhiều người cùng mua 1 món hàng cuối cùng lúc.
            Product product = productRepository.findByIdWithLock(item.getProduct().getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại!"));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " đã ngừng bán!");
            }

            //Kiểm tra số lượng tồn kho
            if (product.getStock() < item.getQuantity()){
                throw new BadRequestException("Sản phẩm " + product.getName() + " đã hết hàng hoặc không đủ số lượng trong kho");
            }

            //Trừ số lượng kho thực tế và lưu lại
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            // Tạo dữ liệu sản phẩm tại thời điểm checkout (từ Product + CartItem)
            // → đây là object mẫu (prototype)
            OrderItemPrototype productData = OrderItemPrototype.from(product, item.getQuantity());

            // Clone object mẫu để tạo snapshot độc lập
            // → dữ liệu này sẽ không bị ảnh hưởng nếu Product thay đổi sau này
            OrderItemPrototype snapshotData = productData.clone();

            // Tạo OrderItem từ snapshot để lưu vào database
            // → đảm bảo lưu đúng tên và giá tại thời điểm khách đặt hàng
            OrderItem orderItem = snapshotData.toOrderItem(savedOrder, product);
            orderItemRepository.save(orderItem);


            // Tính tổng tiền dựa trên snapshot (không dùng trực tiếp Product)
            // → đảm bảo nhất quán với dữ liệu đã lưu trong OrderItem
            BigDecimal itemTotal = snapshotData.getPrice().multiply(BigDecimal.valueOf(snapshotData.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        //5. Cập nhật lại tổng tiền cuối cùng cho đơn hàng
        savedOrder.setTotalPrice(totalAmount);
        orderRepository.save(savedOrder);

        //6. Tạo bản ghi thanh toán tương ứng với đơn hàng
        // Payment ban đầu luôn ở trạng thái PENDING.
        // Với VNPay, trạng thái sẽ được cập nhật sau callback.
        // Với COD, trạng thái được xác nhận trong quá trình xử lý/giao hàng.
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(parsePaymentMethod(request.getPaymentMethod()));
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        //7. Xóa sạch giỏ hàng sau khi đặt hàng thành công
        cartItemRepository.deleteByCartId(cart.getCartId());
        
        //8. Đóng gói dữ liệu trả về cho Client
        return OrderResponseDTO.builder()
                .orderId(savedOrder.getOrderId())
                .status(savedOrder.getStatus().name())
                .totalPrice(savedOrder.getTotalPrice())
                .recipientName(savedOrder.getRecipientName())
                .recipientPhone(savedOrder.getRecipientPhone())
                .shippingAddress(savedOrder.getShippingAddress())
                .createdAt(savedOrder.getCreatedAt())
                .message("Đặt hàng thành công. Mã đơn hàng của bạn là #" + savedOrder.getOrderId())
                .build();
    }

    /**
     * Lấy danh sách các đơn hàng của người dùng hiện tại.
     *
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng được sắp xếp theo thời gian tạo giảm dần
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(Integer userId) {

        validateUserId(userId);
        //Tìm đơn hàng theo userId, sắp xếp mới nhất lên đầu và map sang DTO
        return orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> OrderResponseDTO.builder()
                                .orderId(order.getOrderId())
                                .status(order.getStatus().name())
                                .totalPrice(order.getTotalPrice())
                                .recipientName(order.getRecipientName())
                                .createdAt(order.getCreatedAt())
                                .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách đơn hàng dành cho Admin, hỗ trợ lọc và phân trang.
     *
     * @param status  Trạng thái đơn hàng cần lọc - không bắt buộc
     * @param keyword Từ khóa tìm kiếm theo tên người nhận hoặc số điện thoại - không bắt buộc
     * @param page    Số trang cần lấy
     * @param size    Số lượng bản ghi trên mỗi trang
     * @return Danh sách đơn hàng dạng phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderListDTO> getAdminOrders(String status, String keyword, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Số trang không hợp lệ!");
        }

        if (size <= 0) {
            throw new BadRequestException("Kích thước trang không hợp lệ!");
        }

        //Cấu hình phân trang, mặc định sắp xếp theo ngày tạo giảm dần (mới nhất trước)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        //Chuyển trạng thái từ String sang Enum
        OrderStatus orderStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái đơn hàng không hợp lệ!");
            }
        }

        //Gọi query để tìm kiếm và lọc
        Page<Order> orderPage = orderRepository.searchAdminOrders(orderStatus, keyword, pageable);

        //Chuyển đổi từ entity Order sang dto OrderListDTO để trả về
        return orderPage.map(order -> OrderListDTO.builder()
                .orderId(order.getOrderId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build()
            );
    }

    /**
     * Lấy thông tin chi tiết của một đơn hàng dành cho Admin.
     *
     * Thông tin trả về bao gồm:
     * - Thông tin chung của đơn hàng
     * - Danh sách sản phẩm trong đơn
     * - Thông tin thanh toán
     *
     * Xử lý thêm các trường hợp:
     * - Không tìm thấy đơn hàng
     * - Sản phẩm trong đơn đã bị xóa khỏi hệ thống
     * - Đơn hàng chưa có thông tin thanh toán
     *
     * @param orderId ID của đơn hàng cần xem chi tiết
     * @return Thông tin chi tiết đầy đủ của đơn hàng
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponseDTO getOrderDetailForAdmin(Integer orderId) {
        validateOrderId(orderId);

        Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại hoặc đã bị xóa"));
        
        List<OrderItemDTO> items = orderItemRepository.findByOrder(order)
            .stream()
            .map(this::mapToOrderItemDTO)
            .collect(Collectors.toList());

        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderId(orderId);

        return buildOrderDetailResponse(order, paymentOpt, items);
    }

    /**
     * Cập nhật trạng thái đơn hàng cho Admin.
     *
     * Flow xử lý:
     * - Validate request
     * - Parse trạng thái mới
     * - Kiểm tra chuyển trạng thái hợp lệ
     * - Check thanh toán nếu chuyển sang COMPLETED
     * - Hoàn kho nếu chuyển sang CANCELLED
     * - Cập nhật trạng thái và lưu DB
     *
     * @param orderId ID của đơn hàng cần cập nhật
     * @param request Dữ liệu chứa trạng thái mới (status)
     * @return DTO chứa:
     *         - trạng thái cũ
     *         - trạng thái mới
     *         - message kết quả
     *         - danh sách trạng thái tiếp theo (dùng cho FE)
     */
    @Override
    @Transactional
    public OrderStatusUpdateResponseDTO updateOrderStatus(Integer orderId, UpdateOrderStatusRequestDTO request) {
        validateOrderId(orderId);
        validateUpdateStatusRequest(request);
        Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại hoặc đã bị xóa!"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = parseOrderStatus(request.getStatus());

        if (currentStatus == newStatus) {
            throw new BadRequestException("Đơn hàng đang ở trạng thái " + currentStatus.name());
        }

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            validatePaymentForCompletion(orderId);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restockProductsIfNeeded(order);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return OrderStatusUpdateResponseDTO.builder()
                .orderId(updatedOrder.getOrderId())
                .oldStatus(currentStatus.name())
                .newStatus(updatedOrder.getStatus().name())
                .message(buildUpdateStatusMessage(newStatus))
                .availableNextStatus(getAvailableNextStatuses(updatedOrder.getStatus()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponseDTO getOrderDetailForCustomer(Integer userId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại hoặc đã bị xóa!"));
        if (order.getUser() == null|| !order.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem đơn hàng này");
        }

        List<OrderItemDTO> items = orderItemRepository.findByOrder(order)
            .stream()
            .map(this::mapToOrderItemDTO)
            .collect(Collectors.toList());

        Optional<Payment> paymentOpt = paymentRepository.findByOrder_OrderId(orderId);
        return buildOrderDetailResponse(order, paymentOpt, items);
    }

    /**
     * Chuyển đổi từ OrderItem Entity sang OrderItemDTO.
     *
     * Ngoài việc mapping dữ liệu cơ bản, method này còn:
     * - Tính thành tiền của từng sản phẩm
     * - Kiểm tra sản phẩm đã bị xóa khỏi hệ thống hay chưa
     *
     * @param item Entity chi tiết đơn hàng
     * @return DTO chi tiết sản phẩm trong đơn hàng
     */
    private OrderItemDTO mapToOrderItemDTO(OrderItem item) {
        boolean discontinued = item.getProduct() == null;

        BigDecimal subTotal = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemDTO.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getProduct() != null ? item.getProduct().getProductId() : null)
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subTotal(subTotal)
                .discontinued(discontinued)
                .note(discontinued ? PRODUCT_DISCONTINUED_NOTE : null)
                .build();
    }

    /**
     * Tạo OrderDetailResponseDTO từ dữ liệu Order, Payment và danh sách OrderItemDTO.
     *
     * Nếu đơn hàng có thông tin thanh toán:
     * - Gán đầy đủ thông tin payment
     *
     * Nếu đơn hàng chưa có thông tin thanh toán:
     * - Gán message thông báo phù hợp
     *
     * @param order      Thông tin đơn hàng
     * @param paymentOpt Thông tin thanh toán, có thể rỗng
     * @param items      Danh sách sản phẩm trong đơn
     * @return DTO chi tiết đơn hàng hoàn chỉnh
     */
    private OrderDetailResponseDTO buildOrderDetailResponse(
        Order order,
        Optional<Payment> paymentOpt,
        List<OrderItemDTO> items
    ) {
        OrderDetailResponseDTO.OrderDetailResponseDTOBuilder builder = OrderDetailResponseDTO.builder()
            .orderId(order.getOrderId())
            .customerName(order.getUser() != null ? order.getUser().getName() : null)
            .recipientName(order.getRecipientName())
            .recipientPhone(order.getRecipientPhone())
            .shippingAddress(order.getShippingAddress())
            .status(order.getStatus().name())
            .createdAt(order.getCreatedAt())
            .totalPrice(order.getTotalPrice())
            .items(items);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            builder.hasPayment(true)
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .paymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null)
                .paymentAmount(payment.getAmount())
                .transactionNo(payment.getTransactionNo())
                .paymentDate(payment.getPaymentDate())
                .paymentMessage(null);
        } else {
            builder.hasPayment(false)
                .paymentMethod(null)
                .paymentStatus(null)
                .paymentAmount(null)
                .transactionNo(null)
                .paymentDate(null)
                .paymentMessage(NO_PAYMENT_MESSAGE);
        }

        return builder.build();
    }

    private void validateUpdateStatusRequest(UpdateOrderStatusRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Request cập nhật trạng thái không hợp lệ!");
        }

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new BadRequestException("Trạng thái đơn hàng không được để trống!");
        }
    }

    /**
     * Chuyển đổi String status sang enum OrderStatus.
     *
     * @param status Giá trị trạng thái dạng String từ request
     * @return Giá trị OrderStatus hợp lệ dùng cho xử lý nghiệp vụ
     */
    private OrderStatus parseOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ!");
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Set<OrderStatus> allowedStatuses = VALID_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    
        if (!allowedStatuses.contains(newStatus)) {
            throw new BadRequestException(
                "Chuyển đổi trạng thái không hợp lệ. Đơn hàng đang ở trạng thái "
                + currentStatus.name() + ". không thể chuyển sang " + newStatus.name()
            );
        }
    }

    private void validatePaymentForCompletion(Integer orderId) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
            .orElseThrow(() -> new BadRequestException("Không thể hoàn thành đơn hàng do chưa có thông tin thanh toán!"));
    
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Không thể hoàn thành đơn hàng do chưa xác nhận thanh toán thành công");
        }
    }

    /**
     * Hoàn lại số lượng sản phẩm vào kho khi hủy đơn.
     *
     * @param order Đơn hàng cần xử lý
     * @throws BadRequestException nếu không thể hủy đơn
     *
     * Side effect:
     * - Cập nhật lại stock trong bảng Product
     */
    private void restockProductsIfNeeded(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đơn hàng đã giao thành công hoặc đã hoàn thành!");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Không thể hủy đơn hàng đã thanh toán thành công!");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        for (OrderItem item : orderItems) {
            if (item.getProduct() == null) continue;

            Product lockedProduct = productRepository.findByIdWithLock(item.getProduct().getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại trong hệ thống!"));

            lockedProduct.setStock(lockedProduct.getStock() + item.getQuantity());
            productRepository.save(lockedProduct);
        }
    }

    private String buildUpdateStatusMessage(OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED) {
            return "Hủy đơn thành công và đã hoàn trả sản phẩm vào kho hàng!";
        }
        return "Cập nhật trạng thái thành công";
    }

    /**
     * Lấy danh sách trạng thái tiếp theo hợp lệ.
     *
     * @param currentStatus Trạng thái hiện tại của đơn hàng
     * @return Danh sách trạng thái có thể chuyển tiếp (dùng cho FE hiển thị dropdown)
     */
    private List<String> getAvailableNextStatuses(OrderStatus currenStatus) {
        return VALID_TRANSITIONS.getOrDefault(currenStatus, Collections.emptySet())
                .stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
    }

    private void validateOrderRequest(OrderRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Thông tin đặt hàng không hợp lệ!");
        }

        if (request.getRecipientName() == null || request.getRecipientName().trim().isEmpty()) {
            throw new BadRequestException("Họ tên không được để trống!");
        }

        if (request.getRecipientPhone() == null || !request.getRecipientPhone().matches("^\\d{10}$")) {
            throw new BadRequestException("Số điện thoại không hợp lệ, vui lòng nhập đủ 10 chữ số!");
        }

        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new BadRequestException("Địa chỉ nhận hàng không được để trống!");
        }

        parsePaymentMethod(request.getPaymentMethod());
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new BadRequestException("Phương thức thanh toán không được để trống!");
        }

        try {
            return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
        } catch (IllegalArgumentException e){
            throw new BadRequestException("Phương thức thanh toán không hợp lệ! Chỉ hỗ trợ COD hoặc VNPAY.");
        }
    }

    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new BadRequestException("Người dùng không hợp lệ!");
        }
    }

    private void validateOrderId(Integer orderId) {
        if (orderId == null) {
            throw new BadRequestException("Mã đơn hàng không hợp lệ!");
        }
    }
}
