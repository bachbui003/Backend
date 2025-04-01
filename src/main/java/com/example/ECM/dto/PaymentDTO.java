package com.example.ECM.dto;

import com.example.ECM.model.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private String paymentCode; // Transaction ID
    private Long orderId;
    private Long userId; // Thêm userId để dễ mapping
    private BigDecimal amount; // Cập nhật kiểu dữ liệu từ int → BigDecimal
    private PaymentStatus paymentStatus; // Thêm trạng thái thanh toán
    private LocalDateTime paymentDate; // Thêm ngày thanh toán

    private String vnpTxRef;
    private String vnpTransactionId;
    private String vnpTransactionNo;
}
