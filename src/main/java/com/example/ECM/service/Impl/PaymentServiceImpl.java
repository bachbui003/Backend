package com.example.ECM.service.Impl;

import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;
import com.example.ECM.repository.OrderRepository;
import com.example.ECM.repository.PaymentRepository;
import com.example.ECM.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    public Payment createPayment(Order order) {
        // 🔍 Kiểm tra xem đơn hàng đã có thanh toán thành công chưa
        Optional<Payment> existingPayment = paymentRepository.findByOrder(order);
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Đơn hàng đã được thanh toán. Không thể tạo thanh toán mới.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setAmount(order.getTotalPrice());
        payment.setTransactionId(UUID.randomUUID().toString());

        // Xác định trạng thái thanh toán dựa vào số tiền của đơn hàng
        if (order.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            logger.info("✅ Payment initialized with SUCCESS status.");
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            logger.warn("❌ Payment initialized with FAILED status due to zero or negative amount.");
        }

        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        logger.info("✅ Payment created: TransactionId = {}, Amount = {}, Status = {}",
                savedPayment.getTransactionId(), savedPayment.getAmount(), savedPayment.getPaymentStatus());
        return savedPayment;
    }


    @Override
    @Transactional
    public void updatePaymentStatus(String transactionId, PaymentStatus status, String vnpTransactionStatus, String vnpTxnRef, Long vnpAmount) {
        logger.info("🔄 Cập nhật trạng thái thanh toán cho TransactionId: {}", transactionId);

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + transactionId));

        // 🔥 Ngăn chặn cập nhật nếu đã thành công trước đó
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            logger.warn("⚠️ Payment đã được thanh toán trước đó. Không thể cập nhật.");
            throw new RuntimeException("Giao dịch này đã hoàn thành. Không thể cập nhật trạng thái.");
        }

        logger.info("📩 Phản hồi từ VNPay - Trạng thái: {}, Mã giao dịch: {}, Số tiền: {}", vnpTransactionStatus, vnpTxnRef, vnpAmount);

        // Xác định trạng thái mới của thanh toán dựa vào phản hồi từ VNPay
        PaymentStatus newStatus = "00".equals(vnpTransactionStatus) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        // Cập nhật trạng thái nếu có thay đổi
        if (payment.getPaymentStatus() != newStatus) {
            payment.setPaymentStatus(newStatus);
            logger.info("✅ Cập nhật trạng thái thanh toán thành công: {}", newStatus);
        }

        // Cập nhật thông tin giao dịch từ VNPay
        payment.setVnpTransactionNo(vnpTxnRef);
        payment.setAmount(BigDecimal.valueOf(vnpAmount).divide(BigDecimal.valueOf(100))); // Chuyển về VNĐ
        paymentRepository.save(payment);
        logger.info("💾 Đã cập nhật Payment thành công với TransactionId: {}", transactionId);

        // Nếu thanh toán thành công, cập nhật trạng thái đơn hàng
        if (newStatus == PaymentStatus.SUCCESS) {
            markOrderAsPaid(payment);
        }
    }

    @Transactional
    protected void markOrderAsPaid(Payment payment) {
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus("PAID");  // Cập nhật trạng thái thay vì xóa
            orderRepository.save(order);
            logger.info("✅ Đơn hàng đã được cập nhật trạng thái PAID: OrderId = {}", order.getId());
        } else {
            logger.warn("⚠️ Không tìm thấy đơn hàng để cập nhật trạng thái!");
        }
    }

    @Override
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for transactionId: " + transactionId));
    }

    @Override
    @Transactional
    public void savePayment(Payment payment) {
        paymentRepository.save(payment);
        logger.info("💾 Payment saved: TransactionId = {}, Status = {}", payment.getTransactionId(), payment.getPaymentStatus());
    }
}
