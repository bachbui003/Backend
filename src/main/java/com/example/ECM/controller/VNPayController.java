package com.example.ECM.controller;

import com.example.ECM.service.Impl.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular gọi API
@RestController
@RequestMapping("/api/v1/payments")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    // ✅ 1. API Tạo URL Thanh Toán VNPay (Test bằng Postman)
    @PostMapping("/submitOrder")
    public ResponseEntity<Map<String, String>> submitOrder(
            @RequestParam("amount") int orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl);

        // ✅ Trả về URL để test trên Postman
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", vnpayUrl);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-payment")
    public ResponseEntity<Map<String, Object>> processVNPayPayment(HttpServletRequest request) {
        System.out.println("🔄 VNPay Callback Received!");

        // Chuyển HttpServletRequest thành Map<String, String>
        Map<String, String> paramMap = new HashMap<>();
        for (Enumeration<String> paramNames = request.getParameterNames(); paramNames.hasMoreElements();) {
            String paramName = paramNames.nextElement();
            paramMap.put(paramName, request.getParameter(paramName));
        }

        // Gọi phương thức orderReturn với Map<String, String>
        int paymentStatus = vnPayService.orderReturn(paramMap);
        String status = (paymentStatus == 1) ? "SUCCESS" : "FAILED";

        // Lấy các thông tin khác từ request
        String orderInfo = request.getParameter("vnp_OrderInfo");
        String totalAmount = request.getParameter("vnp_Amount");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");

        // Chuẩn bị dữ liệu trả về
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("orderId", orderInfo);
        response.put("totalPrice", Long.parseLong(totalAmount) / 100); // VNPay trả về số tiền tính bằng đồng, cần chia cho 100
        response.put("paymentTime", paymentTime);
        response.put("transactionId", transactionId);

        // Trả về kết quả
        return ResponseEntity.ok(response);
    }

}
