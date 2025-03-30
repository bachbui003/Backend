package com.example.ECM.controller;

import com.example.ECM.service.Impl.PaymentServiceImpl;
import com.example.ECM.service.Impl.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/payments")
public class VNPayController {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private PaymentServiceImpl paymentService;

    // ✅ 1. API Tạo URL Thanh Toán VNPay (Test bằng Postman)
    @PostMapping("/submitOrder")
    public ResponseEntity<Map<String, String>> submitOrder(
            @RequestParam("amount") int orderTotal,
            @RequestParam("orderInfo") String orderInfo,
            HttpServletRequest request) {

        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl);

            // ✅ Trả về URL để test trên Postman
            Map<String, String> response = new HashMap<>();
            response.put("paymentUrl", vnpayUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate payment URL"));
        }
    }

    // ✅ 2. API Nhận Callback từ VNPay
    @GetMapping("/vnpay-payment")
    public ResponseEntity<Map<String, Object>> processVNPayPayment(HttpServletRequest request) {
        try {
            System.out.println("🔄 VNPay Callback Received!");

            Map<String, String[]> paramMap = request.getParameterMap();
            paramMap.forEach((key, value) -> System.out.println(key + ": " + Arrays.toString(value)));

            String responseCode = request.getParameter("vnp_ResponseCode");
            if (responseCode == null) {
                throw new IllegalArgumentException("Response code missing from callback");
            }

            String orderInfo = request.getParameter("vnp_OrderInfo");
            String transactionId = request.getParameter("vnp_TransactionNo");
            String totalAmount = request.getParameter("vnp_Amount");
            String paymentTime = request.getParameter("vnp_PayDate");

            // Kiểm tra nếu các tham số quan trọng bị thiếu
            if (orderInfo == null || transactionId == null || totalAmount == null || paymentTime == null) {
                throw new IllegalArgumentException("Missing parameters in the callback");
            }

            // Xử lý kết quả thanh toán
            int paymentStatus = vnPayService.orderReturn(request);
            String status = (paymentStatus == 1) ? "SUCCESS" : "FAILED";

            // Đảm bảo dữ liệu không bị thiếu hoặc sai
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("orderId", orderInfo);
            response.put("totalPrice", Long.parseLong(totalAmount) / 100);  // Đảm bảo giá trị chính xác (vì VNPay trả về giá trị theo đồng)
            response.put("paymentTime", paymentTime);
            response.put("transactionId", transactionId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Lỗi callback: " + e.getMessage());
            return ResponseEntity.status(400).body(Map.of("error", "Invalid callback data"));
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý callback: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
