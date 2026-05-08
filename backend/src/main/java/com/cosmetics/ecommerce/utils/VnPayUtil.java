package com.cosmetics.ecommerce.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility hỗ trợ xử lý các thao tác liên quan đến VNPay.
 *
 * Bao gồm:
 * - Tạo chữ ký bảo mật bằng HMAC SHA512
 * - Build query string để gửi request sang VNPay
 * - Build chuỗi dữ liệu chuẩn hóa để ký (hashData)
 *
 * Ý nghĩa:
 * - Đảm bảo dữ liệu gửi đi không bị thay đổi (integrity)
 * - Hỗ trợ xác thực dữ liệu khi VNPay callback về
 */
public class VnPayUtil {

    /**
     * Tạo chữ ký bảo mật bằng thuật toán HMAC SHA512.
     *
     * Thuật toán:
     * - Nhận vào key (hashSecret) và data (chuỗi cần ký)
     * - Dùng thuật toán HMAC SHA512 để tạo chữ ký
     * - Chuyển kết quả byte[] sang chuỗi hex
     *
     * @param key  Secret key do VNPay cung cấp
     * @param data Chuỗi dữ liệu cần ký (hashData)
     * @return Chuỗi hash dạng hex (chữ ký)
     */
    public static String hmacSHA512(String key, String data) {
        try {
            // Tạo instance của thuật toán HmacSHA512
            Mac hmac512 = Mac.getInstance("HmacSHA512");

             // Tạo secret key từ chuỗi key
            SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
            );

            // Khởi tạo Mac với secret key
            hmac512.init(secretKey);

            // Thực hiện ký dữ liệu → trả về mảng byte
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Chuyển byte[] sang chuỗi hex
            StringBuilder hash = new StringBuilder();

            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }

            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký VNPay", e);
        }
    }

    /**
     * Build query string để gắn vào URL redirect sang VNPay.
     *
     * Quy trình:
     * - Lọc bỏ các param null hoặc rỗng
     * - Sắp xếp param theo key (A → Z)
     * - Encode key và value theo chuẩn URL (UTF-8)
     * - Nối lại dạng: key=value&key=value
     *
     * Ví dụ:
     * vnp_Amount=10000000&vnp_TxnRef=123
     *
     * @param params Map chứa các tham số gửi VNPay
     * @return Chuỗi query string hoàn chỉnh
     */
    public static String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                // Bỏ param null hoặc rỗng
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                // Sort theo key để đảm bảo thứ tự giống khi ký
                .sorted(Map.Entry.comparingByKey())
                // Encode key + value để đảm bảo URL hợp lệ
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&")); // Nối bằng dấu &
    }

    /**
     * Build chuỗi dữ liệu chuẩn hóa để dùng cho việc ký hash (hashData).
     *
     * Quy trình:
     * - Lọc bỏ các param null hoặc rỗng
     * - Sắp xếp param theo key (A → Z)
     * - Encode value (KHÔNG encode key)
     * - Nối lại dạng: key=value&key=value
     *
     * Khác với buildQueryString:
     * - Không encode key
     * - Dùng cho việc tạo chữ ký (không phải URL)
     *
     * @param params Map chứa các tham số
     * @return Chuỗi hashData dùng để ký HMAC
     */
    public static String buildHashData(Map<String, String> params) {
        return params.entrySet().stream()
                // Bỏ param null hoặc rỗng
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey()) // Sort theo key (rất quan trọng để hash consistent)
                .map(e -> e.getKey() // KHÔNG encode key, chỉ encode value
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
