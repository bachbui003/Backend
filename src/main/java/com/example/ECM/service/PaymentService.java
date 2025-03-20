package com.example.ECM.service;

import com.example.ECM.model.Payment;
import com.example.ECM.model.Order;

public interface PaymentService {
    Payment createPayment(Order order);
    void updatePaymentStatus(String transactionId, String status);
}