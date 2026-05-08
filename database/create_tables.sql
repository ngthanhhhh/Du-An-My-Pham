-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS web_store
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- Chọn database web_store để làm việc
USE web_store;

-- Tạm thời tắt kiểm tra khóa ngoại để có thể drop bảng dễ dàng
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS reviews;
-- Bật lại kiểm tra khóa ngoại
SET FOREIGN_KEY_CHECKS = 1;

-- ===============================
-- ROLES
-- ===============================
CREATE TABLE roles (
	-- Khóa chính của bảng role, tự tăng
    role_id INT AUTO_INCREMENT PRIMARY KEY,
	-- Tên quyền (ADMIN, CUSTOMER)
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- ===============================
-- USERS
-- ===============================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
	-- Email đăng nhập (duy nhất)
    email VARCHAR(150) NOT NULL UNIQUE,
	-- Mật khẩu đã mã hóa
    password VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20),
	-- Khóa ngoại liên kết đến bảng roles
    role_id INT NOT NULL,
	-- Trạng thái tài khoản (true = hoạt động)
    is_active BOOLEAN DEFAULT TRUE,
	-- Thời gian tạo tài khoản
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	-- Thời gian cập nhật lần cuối
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
    ON DELETE RESTRICT  -- Không cho xóa Role nếu đang có User
    ON UPDATE CASCADE   -- Cập nhật ID role thì User tự cập nhật theo
);

-- ===============================
-- CATEGORIES
-- ===============================
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ===============================
-- PRODUCTS
-- ===============================
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    description TEXT,
	-- Đường dẫn ảnh sản phẩm
    image VARCHAR(255),
	-- Trạng thái sản phẩm
    -- active = đang bán
    -- inactive = ngừng bán
    status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    category_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
    ON DELETE RESTRICT -- Chặn xóa danh mục nếu còn sản phẩm
    ON UPDATE CASCADE  -- Cập nhật ID danh mục thì sản phẩm cập nhật theo
);

-- Tạo index để tăng tốc tìm kiếm sản phẩm theo category
CREATE INDEX idx_product_category
ON products(category_id);

-- ===============================
-- CART (Giỏ hàng)
-- ===============================
CREATE TABLE cart (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
	-- Nếu user bị xóa thì cart cũng bị xóa
    ON DELETE CASCADE
    ON UPDATE CASCADE  -- Nếu đổi ID user thì ID ở giỏ tự đổi theo
);

-- ===============================
-- CART ITEMS (Sản phẩm trong giỏ)
-- ===============================
CREATE TABLE cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK(quantity > 0),
    CONSTRAINT fk_cartitem_cart
    FOREIGN KEY (cart_id) REFERENCES cart(cart_id)
    ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE CASCADE,
	-- Không cho phép trùng sản phẩm trong cùng cart
    UNIQUE(cart_id, product_id)
);
CREATE INDEX idx_cart_items_product
ON cart_items(product_id);

-- ===============================
-- ORDERS (Đơn hàng)
-- ===============================
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address VARCHAR(255) NOT NULL,
    status ENUM(
        'PENDING',
        'PREPARING',
        'SHIPPING',
        'DELIVERED',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Index giúp truy vấn đơn hàng theo user nhanh hơn
CREATE INDEX idx_orders_user
ON orders(user_id);

-- ===============================
-- ORDER ITEMS (Chi tiết đơn hàng)
-- ===============================
CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
	-- Sản phẩm (có thể null nếu sản phẩm bị xóa)
    product_id INT NULL,
	-- Tên sản phẩm tại thời điểm mua
    product_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL CHECK(quantity > 0),
	-- Giá tại thời điểm mua
    price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_orderitem_order
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
    ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_product
    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
	-- Nếu product bị xóa thì vẫn giữ order
    ON DELETE SET NULL
);

-- Index giúp truy vấn order items nhanh
CREATE INDEX idx_order_items_order
ON order_items(order_id);

-- ===============================
-- PAYMENTS
-- ===============================
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
	-- Mỗi đơn hàng chỉ có 1 thanh toán
    order_id INT NOT NULL UNIQUE,
	-- Phương thức thanh toán
    payment_method ENUM('COD','VNPAY') DEFAULT 'COD',
	-- Mã giao dịch từ VNPay
	transaction_no VARCHAR(100),
	-- Số tiền thanh toán
    amount DECIMAL(12,2) NOT NULL,
	-- Thời gian thanh toán
    payment_date DATETIME NULL DEFAULT NULL,
	-- Trạng thái thanh toán
    status ENUM('PENDING','SUCCESS','FAILED') DEFAULT 'PENDING',
    CONSTRAINT fk_payment_order
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
    ON DELETE CASCADE
);

-- ===============================
-- REVIEWS (Đánh giá sản phẩm)
-- ===============================
CREATE TABLE reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    
    -- Số sao từ 1 đến 5
    rating INT NOT NULL CHECK(rating >= 1 AND rating <= 5),
    
    -- Nội dung bình luận
    comment TEXT,
    
    -- Nhãn "Xác nhận đã mua hàng" (True = Đã mua và đơn hàng COMPLETED)
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    
    -- Admin đánh dấu (VD: NORMAL, NEGATIVE_FEEDBACK, SPAM...)
    admin_flag ENUM('NORMAL', 'NEGATIVE_FEEDBACK', 'ATTENTION_NEEDED') DEFAULT 'NORMAL',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
    
    -- Optional: Nếu muốn 1 user chỉ được đánh giá 1 sản phẩm 1 lần thì mở dòng dưới ra
    -- UNIQUE(user_id, product_id) 
);

-- Index giúp truy xuất danh sách review của 1 sản phẩm và tính sao trung bình siêu tốc độ
CREATE INDEX idx_reviews_product ON reviews(product_id);
-- Index giúp lọc review theo số sao cho phần "Lọc bình luận theo số sao"
CREATE INDEX idx_reviews_rating ON reviews(product_id, rating);