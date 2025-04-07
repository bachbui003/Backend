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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/create/{orderId}")
    @ResponseBody
    public String createPayment(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Đơn hàng không tồn tại: " + orderId);
        }

        String transactionId = UUID.randomUUID().toString();
        Payment payment = paymentService.createPaymentWithTransactionId(order, transactionId);

        String returnUrl = "http://localhost:8080/api/payment/vnpay-return";
        return vnPayService.createOrder(order.getTotalPrice().intValue(), "Thanh toán đơn hàng", returnUrl, transactionId);
    }

    @GetMapping("/vnpay-return")
    public RedirectView paymentReturn(HttpServletRequest request, @RequestParam Map<String, String> params) {
        try {
            System.out.println("📌 Các tham số trả về từ VNPay:");
            params.forEach((key, value) -> System.out.println(key + ": " + value));

            int result = vnPayService.orderReturn(request);
            System.out.println("📌 Kết quả kiểm tra giao dịch: " + result);

            String transactionId = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            BigDecimal vnpAmount = Optional.ofNullable(params.get("vnp_Amount"))
                    .map(value -> new BigDecimal(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                    .orElseThrow(() -> new IllegalStateException("vnp_Amount không được null từ VNPay"));

            String status = "failed";
            String error = null;

            Payment payment = paymentService.getPaymentByTransactionId(transactionId);
            if (payment != null) {
                PaymentDTO paymentDTO = new PaymentDTO();
                paymentDTO.setPaymentCode(transactionId);
                paymentDTO.setOrderId(payment.getOrder().getId());
                paymentDTO.setAmount(vnpAmount);
                paymentDTO.setVnpTransactionId(params.get("vnp_TransactionNo"));
                paymentDTO.setVnpTransactionNo(params.get("vnp_TransactionNo"));
                paymentDTO.setVnpTxRef(params.get("vnp_TxnRef"));
                paymentDTO.setPaymentStatus("00".equals(vnpResponseCode) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

                paymentService.updatePaymentStatus(paymentDTO);
                System.out.println("✅ Cập nhật trạng thái thanh toán: " + paymentDTO.getPaymentStatus());

                if (paymentDTO.getPaymentStatus() == PaymentStatus.SUCCESS) {
                    status = "success";
                }
            } else {
                System.out.println("⚠️ Không tìm thấy Payment với TransactionId: " + transactionId);
                error = "Không tìm thấy Payment với mã giao dịch.";
            }

            // Thêm orderId vào URL redirect
            Long orderId = payment != null ? payment.getOrder().getId() : null;
            String redirectUrl = String.format(
                    "http://localhost:4200/payment-callback?status=%s&transactionId=%s&amount=%s%s%s",
                    status,
                    transactionId,
                    vnpAmount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString(),
                    error != null ? "&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8) : "",
                    orderId != null ? "&orderId=" + orderId : ""
            );

            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            System.out.println("❌ Lỗi xử lý callback: " + e.getMessage());
            String redirectUrl = String.format(
                    "http://localhost:4200/payment-callback?status=failed&transactionId=%s&amount=%s&error=%s",
                    params.get("vnp_TxnRef"),
                    params.get("vnp_Amount") != null
                            ? new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).toBigInteger().toString()
                            : "0",
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
            );
            return new RedirectView(redirectUrl);
        }
    }

    @PostMapping("/create/cod/{orderId}")
    @ResponseBody
    public ResponseEntity<Payment> createCODPayment(@PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                System.out.println("❌ Đơn hàng không tồn tại: " + orderId);
                return ResponseEntity.badRequest().body(null);
            }
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("❌ Đơn hàng không ở trạng thái PENDING: " + orderId);
                return ResponseEntity.badRequest().body(null);
            }

            Payment payment = paymentService.createCODPayment(order);
            System.out.println("✅ Thanh toán COD thành công cho đơn hàng: " + orderId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            System.out.println("❌ Lỗi khi tạo thanh toán COD cho đơn hàng: " + orderId + " - " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
}
