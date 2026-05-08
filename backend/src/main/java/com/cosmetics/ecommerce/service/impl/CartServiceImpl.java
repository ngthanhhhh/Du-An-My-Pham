package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmetics.ecommerce.dto.CartItemRequestDTO;
import com.cosmetics.ecommerce.dto.CartItemResponseDTO;
import com.cosmetics.ecommerce.dto.CartResponseDTO;
import com.cosmetics.ecommerce.entity.Cart;
import com.cosmetics.ecommerce.entity.CartItem;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.enums.ProductStatus;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.CartItemRepository;
import com.cosmetics.ecommerce.repository.CartRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service triển khai các nghiệp vụ liên quan đến giỏ hàng.
 *
 * Bao gồm:
 * - Xem giỏ hàng của người dùng
 * - Thêm sản phẩm vào giỏ hàng
 * - Cập nhật số lượng sản phẩm trong giỏ
 * - Xóa sản phẩm khỏi giỏ hàng
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService{
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final int MAX_QUANTITY_PER_ITEM = 99;

    /**
     * Lấy giỏ hàng của người dùng.
     *
     * Nếu người dùng chưa có giỏ hàng:
     * - Tự động tạo mới giỏ hàng
     *
     * @param userId ID người dùng
     * @return Thông tin giỏ hàng sau khi ánh xạ sang DTO
     */
    @Override
    @Transactional
    public CartResponseDTO getCartByUserId(Integer userId) {
        validateUserId(userId);
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng.
     *
     * Quy trình xử lý:
     * - Kiểm tra người dùng hợp lệ
     * - Kiểm tra request hợp lệ
     * - Kiểm tra sản phẩm tồn tại và còn đang kinh doanh
     * - Lấy hoặc tạo giỏ hàng cho người dùng
     * - Lấy CartItem hiện có hoặc tạo mới nếu sản phẩm chưa có trong giỏ
     * - Tính số lượng mới sau khi cộng thêm
     * - Kiểm tra giới hạn số lượng và tồn kho
     * - Lưu CartItem vào database
     * - Truy xuất lại giỏ hàng mới nhất để trả về cho client
     *
     * @param userId ID của người dùng
     * @param request Thông tin sản phẩm và số lượng cần thêm
     * @return Giỏ hàng sau khi thêm sản phẩm thành công
     */
    @Override
    public CartResponseDTO addToCart(Integer userId, CartItemRequestDTO request) {
        validateUserId(userId);
        validateAddToCartRequest(request);

        Product product = getValidProduct(request.getProductId());
        Cart cart = getOrCreateCart(userId);
        CartItem cartItem = getOrCreateCartItem(cart, product);

        boolean isNewItem = cartItem.getCartItemId() == null;

        int currentQuantity = cartItem.getCartItemId() == null ? 0 : cartItem.getQuantity();
        int newQuantity = currentQuantity + request.getQuantity();
        
        validateQuantityLimits(product, newQuantity);

        cartItem.setQuantity(newQuantity);
        CartItem savedItem = cartItemRepository.save(cartItem);

        if (isNewItem) {
            cart.getCartItems().add(savedItem);
        }

        return mapToCartResponse(cart);
    }

    /**
     * Cập nhật số lượng của một sản phẩm đã có trong giỏ hàng.
     *
     * Quy trình xử lý:
     * - Kiểm tra người dùng hợp lệ
     * - Kiểm tra request hợp lệ
     * - Kiểm tra sản phẩm tồn tại và còn đang kinh doanh
     * - Lấy giỏ hàng của người dùng
     * - Tìm CartItem tương ứng trong giỏ
     * - Kiểm tra số lượng cập nhật có vượt giới hạn hoặc tồn kho không
     * - Cập nhật lại số lượng và lưu vào database
     * - Trả về giỏ hàng sau khi cập nhật
     *
     * @param userId ID của người dùng
     * @param request Thông tin sản phẩm và số lượng mới
     * @return Giỏ hàng sau khi cập nhật thành công
     */
    @Override
    public CartResponseDTO updateCartItem(Integer userId, CartItemRequestDTO request){
        validateUserId(userId);
        validateUpdateCartRequest(request);
        Product product = getValidProduct(request.getProductId());
        Cart cart = getExistingCart(userId);

        CartItem cartItem = cartItemRepository
            .findByCart_CartIdAndProduct_ProductId(cart.getCartId(), product.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm chưa có trong giỏ hàng để cập nhật!"));

        int newQuantity = request.getQuantity();
        validateQuantityLimits(product, newQuantity);

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        Cart refreshedCart = cartRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng!"));
        return mapToCartResponse(refreshedCart);
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng của người dùng.
     *
     * Quy trình xử lý:
     * - Kiểm tra người dùng hợp lệ
     * - Kiểm tra productId hợp lệ
     * - Lấy giỏ hàng hiện tại của người dùng
     * - Tìm CartItem tương ứng trong giỏ
     * - Gỡ CartItem khỏi danh sách trong Cart để đồng bộ object trong memory
     * - Xóa CartItem khỏi database
     *
     * @param userId ID của người dùng
     * @param productId ID của sản phẩm cần xóa khỏi giỏ
     */
    @Override
    public void removeCartItem(Integer userId, Integer productId) {
        validateUserId(userId);
        validateProductId(productId);

        Cart cart = getExistingCart(userId);

        CartItem cartItem = cartItemRepository
            .findByCart_CartIdAndProduct_ProductId(cart.getCartId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng!"));

        // Gỡ khỏi list trong Cart để đồng bộ object trong memory
        if (cart.getCartItems() != null) {
            cart.getCartItems().removeIf(item -> item.getProduct().getProductId().equals(productId));
        }
        cartItemRepository.delete(cartItem);
    }

    /**
     * Lấy giỏ hàng của người dùng hoặc tạo mới nếu chưa tồn tại.
     *
     * Quy trình xử lý:
     * - Tìm Cart theo userId
     * - Nếu chưa có thì kiểm tra User tồn tại
     * - Tạo mới Cart và gắn với User
     *
     * @param userId ID của người dùng
     * @return Giỏ hàng hiện tại của người dùng
     */
    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Chuyển đổi Cart entity sang CartResponseDTO.
     *
     * Bao gồm:
     * - Mapping toàn bộ danh sách CartItem sang DTO
     * - Tính tổng giá trị của giỏ hàng
     *
     * @param cart Giỏ hàng cần chuyển đổi
     * @return DTO chứa thông tin giỏ hàng trả về cho frontend
     */
    private CartResponseDTO mapToCartResponse(Cart cart) {
        List<CartItemResponseDTO> items = Optional.ofNullable(cart.getCartItems())
                .orElse(List.of())
                .stream()
                .map(this::mapToCartItemResponse)
                .toList();
        BigDecimal totalCartValue = items.stream()
                .map(CartItemResponseDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return CartResponseDTO.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser().getUserId())
                .items(items)
                .totalCartValue(totalCartValue)
                .build();                
    }

    /**
     * Chuyển đổi một CartItem entity sang CartItemResponseDTO.
     *
     * Bao gồm:
     * - Lấy thông tin cơ bản của sản phẩm
     * - Tính đơn giá và thành tiền theo số lượng hiện tại
     *
     * @param item CartItem cần chuyển đổi
     * @return DTO chứa thông tin một sản phẩm trong giỏ hàng
     */
    private CartItemResponseDTO mapToCartItemResponse(CartItem item){
        BigDecimal unitPrice = item.getProduct().getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponseDTO.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProduct().getProductId())
                    .productName(item.getProduct().getName())
                    .productImage(item.getProduct().getImage())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();
    }

    /**
     * Kiểm tra tính hợp lệ của request thêm sản phẩm vào giỏ hàng.
     *
     * Điều kiện hợp lệ:
     * - Request không được null
     * - productId không được để trống
     * - quantity phải lớn hơn 0
     *
     * @param request Thông tin thêm vào giỏ hàng
     */
    private void validateAddToCartRequest(CartItemRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Yêu cầu thêm vào giỏ hàng không hợp lệ!");
        }

        if (request.getProductId() == null) {
            throw new BadRequestException("Mã sản phẩm không được để trống!");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng thêm vào giỏ hàng phải lớn hơn 0!");
        }
    }

    /**
     * Kiểm tra tính hợp lệ của request cập nhật giỏ hàng.
     *
     * Điều kiện hợp lệ:
     * - Request không được null
     * - productId không được để trống
     * - quantity phải lớn hơn 0
     *
     * @param request Thông tin cập nhật giỏ hàng
     */
    private void validateUpdateCartRequest(CartItemRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Yêu cầu cập nhật giỏ hàng không hợp lệ!");
        }

        if (request.getProductId() == null) {
            throw new BadRequestException("Mã sản phẩm không được để trống!");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Số lượng cập nhật phải lớn hơn 0!");
        }
    }

    /**
     * Kiểm tra sản phẩm có hợp lệ để thao tác với giỏ hàng hay không.
     *
     * Điều kiện hợp lệ:
     * - Sản phẩm phải tồn tại trong hệ thống
     * - Sản phẩm phải đang ở trạng thái ACTIVE
     *
     * @param productId ID của sản phẩm
     * @return Sản phẩm hợp lệ
     */
    private Product getValidProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm!"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm đã ngừng bán!");
        }
        return product;
    }

    /**
     * Lấy CartItem tương ứng với sản phẩm trong giỏ hàng.
     *
     * Nếu sản phẩm chưa có trong giỏ:
     * - Tạo mới CartItem
     * - Gắn Cart và Product tương ứng
     *
     * @param cart Giỏ hàng hiện tại
     * @param product Sản phẩm cần tìm hoặc thêm mới
     * @return CartItem hiện có hoặc CartItem mới được khởi tạo
     */
    private CartItem getOrCreateCartItem(Cart cart, Product product) {
        return cartItemRepository
                .findByCart_CartIdAndProduct_ProductId(cart.getCartId(), product.getProductId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    return newItem;
                });
    }

    /**
     * Kiểm tra giới hạn số lượng của sản phẩm trong giỏ hàng.
     *
     * Bao gồm:
     * - Không được vượt quá số lượng tối đa cho mỗi loại sản phẩm
     * - Không được vượt quá số lượng tồn kho hiện tại
     *
     * @param product Sản phẩm cần kiểm tra
     * @param targetQuantity Số lượng mong muốn sau khi thêm hoặc cập nhật
     */
    private void validateQuantityLimits(Product product, int targetQuantity) {
        if (targetQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException("Chỉ được mua tối đa " +  MAX_QUANTITY_PER_ITEM + " sản phẩm cho mỗi loại!");
        }
        
        if (product.getStock() < targetQuantity) {
            throw new BadRequestException("Tồn kho không đủ. Chỉ còn " + product.getStock() + " sản phẩm!");
        }
    }

    /**
     * Kiểm tra ID người dùng có hợp lệ không.
     *
     * @param userId ID của người dùng đang thao tác với giỏ hàng
     */
    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new BadRequestException("Người dùng không hợp lệ!");
        }
    }

    /**
     * Kiểm tra ID sản phẩm có hợp lệ không.
     *
     * @param productId ID sản phẩm cần thao tác
     */
    private void validateProductId(Integer productId) {
        if (productId == null) {
            throw new BadRequestException("Mã sản phẩm không được để trống!");
        }
    }

    /**
     * Lấy giỏ hàng hiện có của người dùng.     *
     * @param userId ID người dùng
     * @return Giỏ hàng hiện có của người dùng
     */
    private Cart getExistingCart(Integer userId) {
        return cartRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng!"));
    }
}
