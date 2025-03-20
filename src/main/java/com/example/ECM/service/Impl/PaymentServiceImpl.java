package com.example.ECM.service.Impl;

import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;
import com.example.ECM.repository.PaymentRepository;
import com.example.ECM.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public Payment createPayment(Order order) {
        // Tạo đối tượng Payment từ Order
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser()); // Gán user từ order
        payment.setAmount(order.getTotalPrice());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentStatus(PaymentStatus.PENDING); // Trạng thái ban đầu là PENDING
        payment.setPaymentDate(LocalDateTime.now());

        // Lưu Payment vào DB
        return paymentRepository.save(payment);
    }

    @Override
    public void updatePaymentStatus(String transactionId, String status) {
        // Tìm Payment theo transactionId
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại"));

        try {
            // Chuyển đổi trạng thái thanh toán thành PaymentStatus enum
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            payment.setPaymentStatus(paymentStatus); // Cập nhật trạng thái thanh toán
        } catch (IllegalArgumentException e) {
            // Nếu trạng thái không hợp lệ
            throw new RuntimeException("Trạng thái thanh toán không hợp lệ: " + status);
        }

        // Lưu lại Payment với trạng thái mới
        paymentRepository.save(payment);
    }

}
