package com.example.ECM.service.Impl;

import com.example.ECM.config.VNPayConfig;
import com.example.ECM.model.VNPayResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    public String createOrder(int total, String orderInfor, String urlReturn, String txnRef) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = txnRef; // Sử dụng transactionId từ PaymentController thay vì tạo ngẫu nhiên
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String orderType = "other";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(total * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString())).append("&");
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString())).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString())).append("&");
                }
            }
            hashData.setLength(hashData.length() - 1);
            query.setLength(query.length() - 1);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        return VNPayConfig.vnp_PayUrl + "?" + query.toString();
    }

    public int orderReturn(HttpServletRequest request) {
        // Lấy các tham số từ request
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        // Lấy mã phản hồi từ VNPay
        String responseCode = request.getParameter("vnp_ResponseCode");
        VNPayResponseCode vnpResponse = VNPayResponseCode.fromCode(responseCode);

        System.out.println("Mã phản hồi từ VNPay: " + vnpResponse);

        // Xử lý theo mã phản hồi từ VNPay
        switch (vnpResponse) {
            case SUCCESS:
                return 1; // Thành công
            case USER_CANCELLED:
                System.out.println("Người dùng đã hủy giao dịch.");
                return 0;
            case TRANSACTION_FAILED:
                System.out.println("Giao dịch thất bại.");
                return 0;
            case INVALID_SIGNATURE:
                System.out.println("Chữ ký không hợp lệ.");
                return -1;
            default:
                System.out.println("Lỗi không xác định từ VNPay: " + responseCode);
                return -2;
        }
    }
}