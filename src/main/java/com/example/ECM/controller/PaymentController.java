package com.example.ECM.controller;

import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;
import com.example.ECM.service.OrderService;
import com.example.ECM.service.PaymentService;
import com.example.ECM.service.Impl.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        Order order = orderService.getOrderById(orderId);
        Payment payment = paymentService.createPayment(order);
        String returnUrl = "http://localhost:8080/api/payment/vnpay-return";
        return vnPayService.createOrder(order.getTotalPrice().intValue(), "Thanh toán đơn hàng", returnUrl);
    }

    @GetMapping("/vnpay-return")
    public String paymentReturn(HttpServletRequest request, @RequestParam Map<String, String> params) {
        System.out.println("Các tham số trả về từ VNPay:");
        params.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("vnp_SecureHash: " + request.getParameter("vnp_SecureHash"));
        System.out.println("vnp_ResponseCode: " + request.getParameter("vnp_ResponseCode"));
        System.out.println("vnp_TransactionStatus: " + request.getParameter("vnp_TransactionStatus"));

        int result = vnPayService.orderReturn(request);
        System.out.println("Kết quả kiểm tra giao dịch: " + result);

        if (result == 1) {
            String transactionId = params.get("vnp_TxnRef");
            String vnpTransactionId = params.get("vnp_TransactionNo");
            String vnpTransactionStatus = request.getParameter("vnp_TransactionStatus");
            Long vnpAmount = Long.valueOf(params.get("vnp_Amount"));

            // Cập nhật trạng thái thanh toán thành SUCCESS
            paymentService.updatePaymentStatus(transactionId, PaymentStatus.SUCCESS, vnpTransactionStatus, vnpTransactionId, vnpAmount);
            System.out.println("Cập nhật trạng thái thanh toán: " + PaymentStatus.SUCCESS);

            // Lấy thông tin payment từ database
            Payment payment = paymentService.getPaymentByTransactionId(transactionId);
            if (payment != null) {
                payment.setVnpTransactionId(vnpTransactionId); // Lưu vnpTransactionId
                paymentService.savePayment(payment); // Lưu vào DB

                // Xóa đơn hàng nếu thanh toán thành công
                Long orderId = payment.getOrder().getId();
                System.out.println("Xóa đơn hàng có ID: " + orderId);
                orderService.deleteOrder(orderId); // Xóa đơn hàng
            }
            return "Thanh toán thành công và đơn hàng đã bị xóa";
        } else {
            // Cập nhật trạng thái thanh toán thành FAILED
            paymentService.updatePaymentStatus(params.get("vnp_TxnRef"), PaymentStatus.FAILED, null, null, 0L);
            System.out.println("Cập nhật trạng thái thanh toán: " + PaymentStatus.FAILED);
            return "Thanh toán thất bại";
        }
    }
}
