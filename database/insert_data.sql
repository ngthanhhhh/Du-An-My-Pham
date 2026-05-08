USE web_store;

-- 1. Roles
INSERT INTO roles(role_name) VALUES ('ROLE_ADMIN'), ('ROLE_CUSTOMER');

-- 2. Users (Mật khẩu giả định: 123456)
INSERT INTO users (name, email, password, address, phone, role_id) VALUES 
('Admin Aura', 'admin@aurabeauty.vn', '123456', '97 Man Thiện, Quận 9, HCM', '0901112222', 1),
('Quỳnh Manager', 'quynh@aurabeauty.vn', '123456', 'Thủ Đức, HCM', '0903334444', 1),
('Nguyễn Văn Khách', 'khach1@gmail.com', '123456', 'Quận 1, HCM', '0981112222', 2),
('Lê Thị Thanh', 'thanh.test@gmail.com', '123456', 'Quận 7, HCM', '0983334444', 2),
('Trần Thu Thủy', 'thuy.test@gmail.com', '123456', 'Bình Thạnh, HCM', '0975556666', 2),
('Phạm Minh Tuấn', 'tuan.pham@gmail.com', '123456', 'Hải Châu, Đà Nẵng', '0911222333', 2),
('Hoàng Bảo Ngọc', 'ngoc.hoang@gmail.com', '123456', 'Ba Đình, Hà Nội', '0944555666', 2),
('Đặng Quốc Bảo', 'bao.dang@gmail.com', '123456', 'Ninh Kiều, Cần Thơ', '0966777888', 2);

-- 3. Categories
INSERT INTO categories (name, description) VALUES 
('Làm Sạch', 'Sữa rửa mặt, tẩy trang'),
('Dưỡng Da', 'Serum, kem dưỡng phục hồi'),
('Trang Điểm', 'Son, Cushion, Phấn'),
('Chống Nắng', 'Bảo vệ da toàn diện'),
('Mặt Nạ', 'Mặt nạ giấy, mặt nạ ngủ'),
('Phụ Kiện', 'Bông tẩy trang, máy rửa mặt');

-- 4. Products (Status: ACTIVE/INACTIVE)
INSERT INTO products (name, price, stock, description, image, category_id, status) VALUES 
('Sữa Rửa Mặt Aura B5', 185000, 100, 'Làm sạch dịu nhẹ', 'srm_b5.jpg', 1, 'ACTIVE'),
('Nước Tẩy Trang Aura', 210000, 50, 'Sạch sâu bã nhờn', 'tt.jpg', 1, 'ACTIVE'),
('Tẩy Tế Bào Chết Cafe', 120000, 40, 'Mịn da tự nhiên', 'ttbc.jpg', 1, 'ACTIVE'),
('Serum HA Lấp Lánh', 350000, 80, 'Cấp ẩm đa tầng', 'serum_ha.jpg', 2, 'ACTIVE'),
('Kem Phục Hồi Aura', 420000, 30, 'Tái tạo da ban đêm', 'kem.jpg', 2, 'ACTIVE'),
('Serum Vitamin C 15%', 550000, 0, 'Sáng da mờ thâm', 'serum_c.jpg', 2, 'INACTIVE'),
('Kem Dưỡng Rau Má', 290000, 65, 'Dịu da mụn', 'kem_rauma.jpg', 2, 'ACTIVE'),
('Son Kem Aura #01', 250000, 150, 'Đỏ san hô', 'son_01.jpg', 3, 'ACTIVE'),
('Son Kem Aura #02', 250000, 120, 'Hồng trà', 'son_02.jpg', 3, 'ACTIVE'),
('Cushion Aura Matte', 420000, 45, 'Che phủ hoàn hảo', 'cushion.jpg', 3, 'ACTIVE'),
('Phấn Phủ Aura Silk', 280000, 90, 'Kiềm dầu mịn da', 'phan.jpg', 3, 'ACTIVE'),
('KCN Aura Invisible', 390000, 110, 'Chống nắng SPF50+', 'kcn.jpg', 4, 'ACTIVE'),
('KCN Nâng Tông Pink', 390000, 20, 'Trắng hồng tự nhiên', 'kcn_pink.jpg', 4, 'ACTIVE'),
('Mặt Nạ Ngủ Water', 450000, 35, 'Cấp nước tức thì', 'mask_sleep.jpg', 5, 'ACTIVE'),
('Mặt Nạ Giấy Tràm Trà', 25000, 500, 'Giảm sưng mụn', 'mask_tea.jpg', 5, 'ACTIVE'),
('Bông Tẩy Trang 222m', 45000, 200, 'Bông mềm không xơ', 'bong.jpg', 6, 'ACTIVE');

-- 5. Cart & Cart Items
INSERT INTO cart (user_id) VALUES (1), (3), (4), (5), (6), (7);
INSERT INTO cart_items (cart_id, product_id, quantity) VALUES 
(1, 1, 2), 
(1, 4, 1), 
(2, 8, 1), 
(3, 4, 1),
(3, 16, 2),
(4, 10, 1),
(5, 15, 10);

-- 6. Orders (Status: PENDING, SHIPPING, COMPLETED, CANCELLED)
INSERT INTO orders (user_id, total_price, recipient_name, recipient_phone, shipping_address, status, created_at) VALUES 
(3, 620000, 'Nguyễn Văn Khách', '0981112222', 'Quận 1, HCM', 'COMPLETED', '2026-03-01 09:00:00'),
(4, 440000, 'Lê Thị Thanh', '0983334444', 'Quận 7, HCM', 'COMPLETED', '2026-03-05 14:00:00'),
(6, 390000, 'Phạm Minh Tuấn', '0911222333', 'Đà Nẵng', 'SHIPPING', '2026-03-10 08:30:00'),
(7, 250000, 'Hoàng Bảo Ngọc', '0944555666', 'Hà Nội', 'CANCELLED', '2026-03-11 11:00:00'),
(5, 520000, 'Trần Thu Thủy', '0975556666', 'Bình Thạnh, HCM', 'PENDING', NOW());

-- 7. Order Items
INSERT INTO order_items (order_id, product_id, product_name, quantity, price) VALUES 
(1, 1, 'Sữa Rửa Mặt Aura B5', 2, 185000), (1, 8, 'Son Kem Aura #01', 1, 250000),
(2, 4, 'Serum HA Lấp Lánh', 1, 350000), (2, 16, 'Bông Tẩy Trang 222m', 2, 45000),
(3, 12, 'KCN Aura Invisible', 1, 390000),
(4, 9, 'Son Kem Aura #02', 1, 250000),
(5, 1, 'Sữa Rửa Mặt Aura B5', 1, 185000);

-- 8. Payments (Status: SUCCESS, FAILED, PENDING)
INSERT INTO payments (order_id, payment_method, amount, status) VALUES 
(1, 'VNPAY', 620000, 'SUCCESS'),
(2, 'COD', 440000, 'SUCCESS'),
(3, 'VNPAY', 390000, 'SUCCESS'),
(4, 'COD', 250000, 'FAILED'),
(5, 'VNPAY', 520000, 'PENDING');


-- 9. REVIEWS (Đánh giá sản phẩm)
INSERT INTO reviews (user_id, product_id, rating, comment, is_verified_purchase, admin_flag, created_at) VALUES 
-- =========================================================
-- CASE 1: Đã mua hàng & Giao thành công (is_verified = TRUE)
-- =========================================================
-- User 3 đã mua SP 1 và 8 (Đơn hàng 1 - COMPLETED)
(3, 1, 5, 'Sữa rửa mặt xài rất êm, da không bị khô. Đóng gói cẩn thận, giao hàng nhanh!', TRUE, 'NORMAL', '2026-03-03 10:00:00'),
(3, 8, 4, 'Màu son lên môi chuẩn đẹp, nhưng ăn uống xong thì hơi mau trôi nha shop.', TRUE, 'NORMAL', '2026-03-03 10:05:00'),

-- User 4 đã mua SP 4 và 16 (Đơn hàng 2 - COMPLETED) -> Test 1 sao để ra cờ NEGATIVE_FEEDBACK
(4, 4, 1, 'Xài bị kích ứng, da nổi mẩn đỏ ngứa ngáy quá shop ơi, cần hoàn tiền gấp!!!', TRUE, 'NEGATIVE_FEEDBACK', '2026-03-07 15:30:00'),
(4, 16, 5, 'Bông tẩy trang dai, mịn, xài siêu tiết kiệm nước tẩy trang, rất đáng tiền.', TRUE, 'NORMAL', '2026-03-07 15:35:00'),

-- =========================================================
-- CASE 2: Chưa mua/Đơn chưa hoàn thành (is_verified = FALSE)
-- =========================================================
-- User 5 có đơn SP 1 nhưng đang PENDING (Chưa nhận hàng) 
(5, 1, 5, 'Mình chưa nhận được hàng nhưng thấy bạn bè khen dòng B5 này lắm nên cho 5 sao ủng hộ shop trước.', FALSE, 'NORMAL', NOW()),

-- User 7 có đơn CANCELLED, lên bình luận dạo, hoặc test spam -> Test cờ ATTENTION_NEEDED
(7, 2, 5, 'Sản phẩm này bên mình đang sale rẻ hơn nửa giá, mọi người qua zalo 09xxx mua nha!', FALSE, 'ATTENTION_NEEDED', NOW()),

-- User 6 test tính năng lọc sao (Cho 3 sao)
(6, 12, 3, 'Giá hơi chát so với dung tích, chưa xài thử nên chưa biết chất lượng ra sao.', FALSE, 'NORMAL', NOW());
