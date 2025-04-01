package com.example.ECM.service.Impl;

import com.example.ECM.dto.PaymentDTO;
import com.example.ECM.model.Order;
import com.example.ECM.model.Payment;
import com.example.ECM.model.PaymentStatus;
import com.example.ECM.model.User;
import com.example.ECM.repository.OrderRepository;
import com.example.ECM.repository.PaymentRepository;
import com.example.ECM.repository.UserRepository;
import com.example.ECM.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    public Payment createPayment(Order order) {
        Optional<Payment> existingPayment = paymentRepository.findByOrder(order);
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Đơn hàng đã được thanh toán. Không thể tạo thanh toán mới.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setAmount(order.getTotalPrice());
        // Không tạo transactionId ngẫu nhiên ở đây nữa, sẽ được set từ client hoặc trước khi gửi VNPay
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);
        logger.info("✅ Payment created: TransactionId = {}, Amount = {}, Status = {}",
                savedPayment.getTransactionId(), savedPayment.getAmount(), savedPayment.getPaymentStatus());
        return savedPayment;
    }

    // Thêm phương thức để tạo Payment với transactionId từ client
    public Payment createPaymentWithTransactionId(Order order, String transactionId) {
        Optional<Payment> existingPayment = paymentRepository.findByOrder(order);
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Đơn hàng đã được thanh toán. Không thể tạo thanh toán mới.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setAmount(order.getTotalPrice());
        payment.setTransactionId(transactionId); // Sử dụng transactionId từ client hoặc trước khi gửi VNPay
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);
        logger.info("✅ Payment created with TransactionId: TransactionId = {}, Amount = {}, Status = {}",
                savedPayment.getTransactionId(), savedPayment.getAmount(), savedPayment.getPaymentStatus());
        return savedPayment;
    }

    @Override
    @Transactional
    public void updatePaymentStatus(PaymentDTO paymentDTO) {
        logger.info("🔄 Đang cập nhật trạng thái thanh toán: TransactionId = {}", paymentDTO.getPaymentCode());

        Payment payment = paymentRepository.findByTransactionId(paymentDTO.getPaymentCode())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + paymentDTO.getPaymentCode()));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            logger.warn("⚠️ Giao dịch đã hoàn thành trước đó. Không thể cập nhật.");
            throw new RuntimeException("Giao dịch này đã hoàn thành. Không thể cập nhật trạng thái.");
        }

        logger.info("🔍 Trạng thái nhận từ DTO: {}", paymentDTO.getPaymentStatus());

        // Sử dụng trực tiếp paymentDTO.getPaymentStatus() thay vì so sánh lại với "00"
        PaymentStatus newStatus = paymentDTO.getPaymentStatus();
        if (newStatus == null) {
            logger.error("⚠️ PaymentStatus từ DTO là null. Gán mặc định là FAILED.");
            newStatus = PaymentStatus.FAILED; // Gán mặc định nếu null
        }
        payment.setPaymentStatus(newStatus);

        // Gán các trường VNPay và kiểm tra null
        payment.setVnpTxRef(paymentDTO.getVnpTxRef() != null ? paymentDTO.getVnpTxRef() : "N/A");
        payment.setVnpTransactionId(paymentDTO.getVnpTransactionId() != null ? paymentDTO.getVnpTransactionId() : "N/A");
        payment.setVnpTransactionNo(paymentDTO.getVnpTransactionNo() != null ? paymentDTO.getVnpTransactionNo() : "N/A");

        BigDecimal amount = paymentDTO.getAmount();
        if (amount == null) {
            amount = payment.getAmount() != null ? payment.getAmount() : payment.getOrder().getTotalPrice();
            if (amount == null) {
                throw new IllegalStateException("Amount không thể null cho giao dịch: " + paymentDTO.getPaymentCode());
            }
        }
        payment.setAmount(amount);

        payment.setPaymentDate(paymentDTO.getPaymentDate() != null ? paymentDTO.getPaymentDate() : LocalDateTime.now());

        paymentRepository.save(payment);
        logger.info("✅ Payment cập nhật: TransactionId = {}, Amount = {}, Status = {}, VnpTxRef = {}, VnpTransactionId = {}, VnpTransactionNo = {}",
                payment.getTransactionId(), payment.getAmount(), payment.getPaymentStatus(),
                payment.getVnpTxRef(), payment.getVnpTransactionId(), payment.getVnpTransactionNo());

        if (newStatus == PaymentStatus.SUCCESS) {
            markOrderAsPaid(payment.getOrder());
        }

    }

    @Transactional
    public void updatePaymentStatusFromVNPay(String transactionId, String vnpayStatus, Map<String, String> vnpayParams) {
        logger.info("🔄 Đang cập nhật trạng thái thanh toán từ VNPay: TransactionId = {}", transactionId);

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với mã: " + transactionId));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            logger.warn("⚠️ Giao dịch đã hoàn thành trước đó. Không thể cập nhật.");
            throw new RuntimeException("Giao dịch này đã hoàn thành. Không thể cập nhật trạng thái.");
        }

        PaymentStatus newStatus = "00".equals(vnpayStatus) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setPaymentStatus(newStatus);
        payment.setVnpTxRef(vnpayParams.get("vnp_TxnRef"));
        payment.setVnpTransactionId(vnpayParams.get("vnp_TransactionId"));
        payment.setVnpTransactionNo(vnpayParams.get("vnp_TransactionNo"));

        String vnpAmountStr = vnpayParams.get("vnp_Amount");
        if (vnpAmountStr != null && !vnpAmountStr.isEmpty()) {
            BigDecimal amount = new BigDecimal(vnpAmountStr).divide(new BigDecimal("100"));
            payment.setAmount(amount);
        } else {
            BigDecimal existingAmount = payment.getAmount();
            if (existingAmount == null) {
                BigDecimal orderAmount = payment.getOrder().getTotalPrice();
                if (orderAmount == null) {
                    throw new IllegalStateException("Amount không thể null cho giao dịch: " + transactionId);
                }
                payment.setAmount(orderAmount);
            }
        }

        String vnpPayDate = vnpayParams.get("vnp_PayDate");
        payment.setPaymentDate(vnpPayDate != null ? parseVNPayDate(vnpPayDate) : LocalDateTime.now());

        paymentRepository.save(payment);
        logger.info("✅ Payment cập nhật từ VNPay: TransactionId = {}, Amount = {}, Status = {}",
                payment.getTransactionId(), payment.getAmount(), payment.getPaymentStatus());

        if (newStatus == PaymentStatus.SUCCESS) {
            markOrderAsPaid(payment.getOrder());
        }
    }

    private LocalDateTime parseVNPayDate(String vnpPayDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(vnpPayDate, formatter);
        } catch (Exception e) {
            logger.warn("⚠️ Không thể parse vnp_PayDate: {}, sử dụng thời gian hiện tại", vnpPayDate);
            return LocalDateTime.now();
        }
    }

    @Transactional
    protected void markOrderAsPaid(Order order) {
        if (order != null) {
            order.setStatus("PAID");
            orderRepository.save(order);
            logger.info("✅ Đơn hàng cập nhật trạng thái PAID: OrderId = {}", order.getId());
        } else {
            logger.warn("⚠️ Không tìm thấy đơn hàng để cập nhật trạng thái!");
        }
    }

    @Override
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Payment với TransactionId: " + transactionId));
    }

    @Override
    @Transactional
    public void savePayment(Payment payment) {
        paymentRepository.save(payment);
        logger.info("💾 Payment saved: TransactionId = {}, Status = {}", payment.getTransactionId(), payment.getPaymentStatus());
    }
}