package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.entity.Order;
import com.cosmetics.ecommerce.entity.Payment;
import com.cosmetics.ecommerce.enums.OrderStatus;
import com.cosmetics.ecommerce.enums.PaymentMethod;
import com.cosmetics.ecommerce.enums.PaymentStatus;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.repository.PaymentRepository;
import com.cosmetics.ecommerce.service.PaymentService;
import com.cosmetics.ecommerce.utils.VnPayUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service triển khai các nghiệp vụ liên quan đến thanh toán.
 *
 * Bao gồm:
 * - Tạo URL thanh toán VNPay cho đơn hàng
 * - Xác thực phản hồi trả về từ VNPay
 * - Cập nhật trạng thái thanh toán
 * - Cập nhật trạng thái đơn hàng sau khi thanh toán thành công
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Tạo URL thanh toán VNPay cho một đơn hàng.
     *
     * Quy trình xử lý:
     * - Tìm thông tin thanh toán theo orderId
     * - Kiểm tra đơn hàng chưa thanh toán thành công
     * - Kiểm tra phương thức thanh toán là VNPay
     * - Kiểm tra đơn hàng đang ở trạng thái PENDING
     * - Tạo mã giao dịch duy nhất
     * - Lưu mã giao dịch vào Payment
     * - Tạo bộ tham số gửi sang VNPay
     * - Ký dữ liệu bằng HMAC SHA512
     * - Trả về URL thanh toán hoàn chỉnh
     *
     * @param orderId ID của đơn hàng cần thanh toán
     * @param ipAddress Địa chỉ IP của người dùng tạo thanh toán
     * @return URL thanh toán VNPay
     */
    @Override
    @Transactional
    public String createVnPayPaymentUrl(Integer userId, Integer orderId, String ipAddress) {
        
        if (orderId == null) {
            throw new BadRequestException("Mã đơn hàng không hợp lệ");
        }

        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = "127.0.0.1";
        }
        //Tìm thông tin thanh toán gắn với đơn hàng.
        // Nếu không có Payment thì không thể tạo link VNPay.
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        // Nếu payment đã thành công rồi thì chặn tạo link mới,
        // tránh người dùng thanh toán lại cùng một đơn hàng.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Đơn hàng đã được thanh toán thành công");
        }

        // Chỉ các đơn chọn phương thức VNPay mới được đi qua flow này.
        // Nếu là COD hoặc phương thức khác thì không tạo URL VNPay.
        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BadRequestException("Đơn hàng này không sử dụng phương thức thanh toán VNPay");
        }

        // Lấy đơn hàng từ payment để kiểm tra trạng thái đơn.
        Order order = payment.getOrder();

        // Nếu payment không liên kết được với order thì dữ liệu bị lỗi.
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng!");
        }

        if (!order.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đơn hàng này!");
        }

        // Chỉ cho thanh toán khi đơn hàng đang ở trạng thái PENDING.
        // Các trạng thái khác như PREPARING, SHIPPING, CANCELLED... không được thanh toán lại.
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể thanh toán đơn hàng đang ở trạng thái PENDING");
        }

        // Tạo mã giao dịch duy nhất gửi sang VNPay.
        // Ghép orderId với thời gian hiện tại để tránh trùng mã giữa nhiều lần thanh toán.
        String txnRef = orderId + "-" + System.currentTimeMillis();
        
        // Lưu mã giao dịch vào Payment để khi VNPay redirect về,
        // hệ thống dùng vnp_TxnRef tìm lại đúng Payment này.
        payment.setTransactionNo(txnRef);

        // Đánh dấu payment đang ở trạng thái chờ thanh toán.
        payment.setStatus(PaymentStatus.PENDING);

        // Lưu Payment trước khi redirect sang VNPay.
        paymentRepository.save(payment);

        // Tạo thời gian theo timezone GMT+7 để gửi cho VNPay.
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        
        // VNPay yêu cầu format thời gian là yyyyMMddHHmmss.
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        // Tạo map chứa toàn bộ tham số gửi sang VNPay.
        Map<String, String> params = new HashMap<>();
        
        // Phiên bản API VNPay.
        params.put("vnp_Version", "2.1.0");

        // Lệnh thanh toán.
        params.put("vnp_Command", "pay");

        // Mã website/merchant do VNPay cấp.
        params.put("vnp_TmnCode", tmnCode);

        // Số tiền thanh toán.
        // VNPay yêu cầu amount = số tiền VND * 100.
        params.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        
        // Đơn vị tiền tệ.
        params.put("vnp_CurrCode", "VND");
        
        // Mã giao dịch duy nhất của hệ thống.
        params.put("vnp_TxnRef", txnRef);
        
        // Nội dung thanh toán hiển thị bên VNPay.
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        
        //Loại đơn hàng
        params.put("vnp_OrderType", "other");
        
        // Ngôn ngữ giao diện VNPay.
        params.put("vnp_Locale", "vn");

        // URL VNPay sẽ redirect về sau khi user thanh toán.
        params.put("vnp_ReturnUrl", returnUrl);

        // IP của người thanh toán.
        params.put("vnp_IpAddr", ipAddress);

        // Thời điểm tạo giao dịch.
        params.put("vnp_CreateDate", formatter.format(calendar.getTime()));

        // Build chuỗi dữ liệu để ký.
        // Util sẽ sort params theo key rồi nối thành key=value&key=value...
        String hashData = VnPayUtil.buildHashData(params);

        // Ký hashData bằng thuật toán HMAC SHA512 và hashSecret.
        // secureHash dùng để VNPay kiểm tra request có bị sửa hay không.
        String secureHash = VnPayUtil.hmacSHA512(hashSecret, hashData);

        // Build query string để gắn lên URL redirect sang VNPay.
        String queryUrl = VnPayUtil.buildQueryString(params);

        // Trả về URL thanh toán hoàn chỉnh.
        // User mở URL này để sang trang thanh toán VNPay.
        return payUrl + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xử lý phản hồi trả về từ VNPay sau khi người dùng thanh toán.
     *
     * Quy trình xử lý:
     * - Lấy chữ ký VNPay gửi về
     * - Loại bỏ các field chữ ký khỏi dữ liệu cần kiểm tra
     * - Tính lại chữ ký bằng hashSecret
     * - So sánh chữ ký để xác thực phản hồi
     * - Tìm Payment theo mã giao dịch VNPay
     * - Nếu giao dịch thành công thì cập nhật Payment SUCCESS
     * - Cập nhật đơn hàng sang trạng thái PREPARING
     * - Nếu giao dịch thất bại thì cập nhật Payment FAILED
     *
     * @param params Toàn bộ tham số VNPay redirect về hệ thống
     * @return Thông báo kết quả xử lý thanh toán
     */
    @Override
    @Transactional
    public String handleVnPayReturn(Map<String, String> params) {

        if (params == null || params.isEmpty()) {
            throw new BadRequestException("Dữ liệu phản hồi VNPay không hợp lệ!");
        }
        
        // Lấy chữ ký VNPay gửi về trong callback/redirect.
        // Chữ ký này dùng để xác thực dữ liệu trả về có hợp lệ không.
        String receivedHash = params.get("vnp_SecureHash");

        // Nếu không có chữ ký thì không thể xác thực phản hồi.
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new BadRequestException("Thiếu chữ ký phản hồi từ VNPay");
        }

        // Copy toàn bộ params sang map mới để chuẩn bị verify.
        // Không sửa trực tiếp params gốc.
        Map<String, String> verifyParams = new HashMap<>(params);

        // Khi tính lại hash, phải bỏ vnp_SecureHash ra.
        // Vì chữ ký không được tự tham gia vào dữ liệu cần ký.
        verifyParams.remove("vnp_SecureHash");

        // Một số response có vnp_SecureHashType, field này cũng không dùng để tính hash.
        verifyParams.remove("vnp_SecureHashType");

        // Build lại chuỗi dữ liệu từ params VNPay gửi về.
        // Quy tắc phải giống lúc tạo URL: sort key và nối key=value.
        String hashData = VnPayUtil.buildHashData(verifyParams);

        // Tính lại chữ ký bằng hashSecret của hệ thống.
        String calculatedHash = VnPayUtil.hmacSHA512(hashSecret, hashData);

        // So sánh chữ ký tự tính với chữ ký VNPay gửi về.
        // Nếu khác nhau nghĩa là dữ liệu có thể đã bị sửa hoặc không hợp lệ.
        if (!calculatedHash.equalsIgnoreCase(receivedHash)) {
            throw new BadRequestException("Chữ ký VNPay không hợp lệ");
        }

        // Lấy mã giao dịch đã gửi sang VNPay lúc tạo URL.
        // Mã này dùng để tìm lại Payment trong database.
        String txnRef = params.get("vnp_TxnRef");

        // Lấy mã kết quả thanh toán.
        // Theo VNPay, "00" thường là giao dịch thành công.
        String responseCode = params.get("vnp_ResponseCode");

        if (responseCode == null || responseCode.isBlank()) {
            throw new BadRequestException("Thiếu mã kết quả thanh toán VNPay");
        }

        // Nếu thiếu mã giao dịch thì không biết callback này thuộc payment nào.
        if (txnRef == null || txnRef.isBlank()) {
            throw new BadRequestException("Thiếu mã giao dịch VNPay");
        }

        // Tìm Payment theo transactionNo đã lưu trước đó.
        Payment payment = paymentRepository.findByTransactionNo(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán"));

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BadRequestException("Giao dịch không thuộc phương thức thanh toán VNPay");
        }

        // Nếu giao dịch đã SUCCESS trước đó thì không update lại nữa.
        // Mục đích: chống callback lặp / user reload return URL.
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return "Giao dịch đã được cập nhật trước đó!";
        }

        // Lấy đơn hàng liên kết với payment.
        Order order = payment.getOrder();
        if (order == null) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của giao dịch thanh toán");
        }
        // Nếu responseCode = "00" -> thanh toán thành công.
        if ("00".equals(responseCode)) {
            // Cập nhật trạng thái thanh toán thành SUCCESS.
            payment.setStatus(PaymentStatus.SUCCESS);

            // Lưu thời điểm thanh toán thành công.
            payment.setPaymentDate(LocalDateTime.now());

            // Sau khi thanh toán thành công, đơn hàng chuyển sang bước chuẩn bị hàng.
            order.setStatus(OrderStatus.PREPARING);

            // Lưu thay đổi payment.
            paymentRepository.save(payment);

            // Lưu thay đổi order.
            orderRepository.save(order);

            // Trả message cho controller/frontend.
            return "Thanh toán thành công";
        }

        // Nếu responseCode khác "00" thì xem như thanh toán thất bại.
        payment.setStatus(PaymentStatus.FAILED);

        // Lưu trạng thái thất bại vào database.
        paymentRepository.save(payment);

        // Trả message thất bại.
        return "Thanh toán thất bại";
    }
}
