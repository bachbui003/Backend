package com.example.ECM.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.net.URLDecoder;


@Component
public class VNPayConfig {
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "https://4224-103-199-76-195.ngrok-free.app/api/v1/payments/vnpay-payment";
    public static String vnp_TmnCode = "5FS5UGAL";
    public static String vnp_HashSecret = "AJIJ3Z1LH6B0D0LOHG4SEED9G3DHFKUT";
    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    // Hàm tạo chữ ký HMAC-SHA512
    public static String hmacSHA512(final String key, final String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] hash = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi sang chuỗi hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            System.err.println("Error in HMACSHA512: " + ex.getMessage());
            return null;
        }
    }

    // Hàm hash tất cả các trường (sắp xếp và tạo chuỗi dữ liệu)
    public static String hashAllFields(Map<String, String> fields) {
        // Sắp xếp theo key (thứ tự alphabet)
        SortedMap<String, String> sortedParams = new TreeMap<>(fields);
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = sortedParams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (iterator.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    // Kiểm tra chữ ký của VNPay
    public static boolean checkSignature(Map<String, String> vnpParams) {
        try {
            // Lấy chữ ký từ VNPay
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                System.err.println("Missing vnp_SecureHash in request parameters");
                return false;
            }

            // Loại bỏ vnp_SecureHash và vnp_SecureHashType khỏi danh sách tham số
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            // Decode giá trị tham số trước khi hash
            for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
                vnpParams.put(entry.getKey(), URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8));
            }

            // Tạo chuỗi dữ liệu để hash
            String hashData = hashAllFields(vnpParams);

            // Tạo chữ ký mới từ dữ liệu hash
            String generatedHash = hmacSHA512(vnp_HashSecret, hashData);

            // Debug log để kiểm tra lỗi
            System.out.println("HashData gửi đi: " + hashData);
            System.out.println("Generated Hash: " + generatedHash);
            System.out.println("VNPay Hash: " + vnp_SecureHash);

            // So sánh chữ ký, bỏ qua chữ hoa/chữ thường
            boolean isValid = vnp_SecureHash.equalsIgnoreCase(generatedHash);
            if (!isValid) {
                System.err.println("❌ Signature mismatch: VNPay hash: " + vnp_SecureHash + " | Generated hash: " + generatedHash);
            }
            return isValid;
        } catch (Exception ex) {
            System.err.println("Error in checkSignature: " + ex.getMessage());
            return false;
        }
    }


    // Lấy địa chỉ IP từ request
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        return (ipAddress != null) ? ipAddress : request.getRemoteAddr();
    }

    // Tạo số ngẫu nhiên
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
