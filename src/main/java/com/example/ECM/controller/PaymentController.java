package com.example.ECM.controller;

import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;  // Import enum PaymentStatus
import com.example.ECM.service.OrderService;
import com.example.ECM.service.PaymentService;
import com.example.ECM.service.Impl.VNPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular gọi API
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/create/{orderId}")
    public String createPayment(@PathVariable Long orderId) {
        // Lấy thông tin đơn hàng từ ID
        Order order = orderService.getOrderById(orderId);

        // Tạo Payment cho đơn hàng
        Payment payment = paymentService.createPayment(order);

        // Chuyển đến trang VNPay sau khi tạo đơn hàng
        String returnUrl = "http://localhost:8080/api/payment/vnpay-return";
        return vnPayService.createOrder(order.getTotalPrice().intValue(), "Thanh toán đơn hàng", returnUrl);
    }

    @GetMapping("/vnpay-return")
    public String paymentReturn(@RequestParam Map<String, String> params) {
        // Lấy transactionId từ các tham số trả về từ VNPay
        String transactionId = params.get("vnp_TxnRef");

        // Kiểm tra kết quả thanh toán từ VNPay
        int result = vnPayService.orderReturn(params);  // Truyền params vào phương thức

        // Cập nhật trạng thái thanh toán dựa vào kết quả
        if (result == 1) {
            // Thanh toán thành công
            paymentService.updatePaymentStatus(transactionId, PaymentStatus.SUCCESS.name());
            return "Thanh toán thành công";
        } else {
            // Thanh toán thất bại
            paymentService.updatePaymentStatus(transactionId, PaymentStatus.FAILED.name());
            return "Thanh toán thất bại";
        }
    }

}
