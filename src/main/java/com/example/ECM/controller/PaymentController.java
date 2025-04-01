package com.example.ECM.controller;

import com.example.ECM.dto.PaymentDTO;
import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;
import com.example.ECM.service.OrderService;
import com.example.ECM.service.PaymentService;
import com.example.ECM.service.Impl.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        String transactionId = UUID.randomUUID().toString(); // Tạo transactionId trước
        Payment payment = paymentService.createPaymentWithTransactionId(order, transactionId); // Sử dụng phương thức mới
        String returnUrl = "http://localhost:8080/api/payment/vnpay-return";

        // Truyền transactionId làm vnp_TxnRef cho VNPay
        return vnPayService.createOrder(order.getTotalPrice().intValue(), "Thanh toán đơn hàng", returnUrl, transactionId);
    }

    @GetMapping("/vnpay-return")
    public String paymentReturn(HttpServletRequest request, @RequestParam Map<String, String> params) {
        System.out.println("📌 Các tham số trả về từ VNPay:");
        params.forEach((key, value) -> System.out.println(key + ": " + value));

        int result = vnPayService.orderReturn(request);
        System.out.println("📌 Kết quả kiểm tra giao dịch: " + result);

        String transactionId = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        BigDecimal vnpAmount = Optional.ofNullable(params.get("vnp_Amount"))
                .map(value -> new BigDecimal(value).divide(new BigDecimal("100")))
                .orElseThrow(() -> new IllegalStateException("vnp_Amount không được null từ VNPay"));

        Payment payment = paymentService.getPaymentByTransactionId(transactionId);
        if (payment != null) {
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setPaymentCode(transactionId);
            paymentDTO.setOrderId(payment.getOrder().getId());
            paymentDTO.setAmount(vnpAmount);
            // Gán tất cả giá trị trước
            paymentDTO.setVnpTransactionId(params.get("vnp_TransactionNo"));
            paymentDTO.setVnpTransactionNo(params.get("vnp_TransactionNo"));
            paymentDTO.setVnpTxRef(params.get("vnp_TxnRef"));
            // Gán trực tiếp PaymentStatus
            paymentDTO.setPaymentStatus("00".equals(vnpResponseCode) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

            paymentService.updatePaymentStatus(paymentDTO);
            System.out.println("✅ Cập nhật trạng thái thanh toán: " + paymentDTO.getPaymentStatus());

            if (paymentDTO.getPaymentStatus() == PaymentStatus.SUCCESS) {
                Long orderId = payment.getOrder().getId();
                System.out.println("✅ Xóa đơn hàng có ID: " + orderId);
                orderService.deleteOrder(orderId);
                return "Thanh toán thành công và đơn hàng đã bị xóa";
            }
        } else {
            System.out.println("⚠️ Không tìm thấy Payment với TransactionId: " + transactionId);
            PaymentDTO failedPaymentDTO = new PaymentDTO();
            failedPaymentDTO.setPaymentCode(transactionId);
            failedPaymentDTO.setAmount(vnpAmount);
            failedPaymentDTO.setPaymentStatus(PaymentStatus.FAILED);
            // Gán giá trị mặc định nếu null
            failedPaymentDTO.setVnpTransactionId(params.get("vnp_TransactionNo"));
            failedPaymentDTO.setVnpTransactionNo(params.get("vnp_TransactionNo"));
            failedPaymentDTO.setVnpTxRef(params.get("vnp_TxnRef"));
            paymentService.updatePaymentStatus(failedPaymentDTO);
        }

        System.out.println("⚠️ Cập nhật trạng thái thanh toán: FAILED");
        return "Thanh toán thất bại";
    }
}