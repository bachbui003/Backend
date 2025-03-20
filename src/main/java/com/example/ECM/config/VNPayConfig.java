package com.example.ECM.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class VNPayConfig {
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "https://4224-103-199-76-195.ngrok-free.app/api/v1/payments/vnpay-payment";
    public static String vnp_TmnCode = "5FS5UGAL";
    public static String vnp_HashSecret = "AJIJ3Z1LH6B0D0LOHG4SEED9G3DHFKUT";
    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    // Hàm MD5
    public static String md5(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            digest = "MD5 Error: " + ex.getMessage();
        }
        return digest;
    }

    // Hàm SHA-256
    public static String Sha256(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            digest = "SHA-256 Error: " + ex.getMessage();
        }
        return digest;
    }

    // Hàm hash tất cả các trường
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                sb.append(fieldName).append("=").append(fieldValue);
            }
            if (fieldNames.indexOf(fieldName) != fieldNames.size() - 1) {
                sb.append("&");
            }
        }
        // Sử dụng SHA256 thay vì HMACSHA512
        return hmacSHA512(vnp_HashSecret, sb.toString());

    }

    // Hàm tạo chữ ký HMACSHA512
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key or data is null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            // Log lỗi chi tiết hơn để tiện cho việc debug
            System.err.println("Error in HMACSHA512: " + ex.getMessage());
            return "HMACSHA512 Error: " + ex.getMessage();
        }
    }

    // Hàm lấy địa chỉ IP từ request
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getLocalAddr();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP: " + e.getMessage();
        }
        return ipAddress;
    }

    // Hàm tạo số ngẫu nhiên
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Kiểm tra chữ ký
    public static boolean checkSignature(HttpServletRequest request, Map<String, String> vnpParams) {
        try {
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                System.err.println("Missing vnp_SecureHash in request parameters");
                return false;
            }

            vnpParams.remove("vnp_SecureHash");  // Loại bỏ chữ ký khỏi các tham số

            // Tạo lại chuỗi hash từ các tham số
            String hashData = hashAllFields(vnpParams);

            // Sử dụng HMACSHA512 để tạo chữ ký
            String secureHash = hmacSHA512(vnp_HashSecret, hashData);

            // So sánh chữ ký của VNPay với chữ ký của chúng ta
            boolean isValid = vnp_SecureHash.equals(secureHash);
            if (!isValid) {
                System.err.println("Signature mismatch: VNPay hash: " + vnp_SecureHash + " | Generated hash: " + secureHash);
            }
            return isValid;
        } catch (Exception ex) {
            System.err.println("Error in checkSignature: " + ex.getMessage());
            return false;
        }
    }


}

