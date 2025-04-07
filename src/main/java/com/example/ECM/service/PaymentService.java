package com.example.ECM.service;

import com.example.ECM.dto.PaymentDTO;
import com.example.ECM.model.Payment;
import com.example.ECM.model.Order;
import com.example.ECM.model.PaymentStatus;

public interface PaymentService {
    Payment createPayment(Order order);
    Payment createPaymentWithTransactionId(Order order, String transactionId); // Thêm phương thức mới
    void updatePaymentStatus(PaymentDTO paymentDTO);
    Payment getPaymentByTransactionId(String transactionId);
    void savePayment(Payment payment);
    Payment createCODPayment(Order order); // Thêm phương thức mới
}