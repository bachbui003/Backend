package com.example.ECM.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal amount;


    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING) // Sử dụng EnumType.STRING để lưu trữ tên enum
    private PaymentStatus paymentStatus;

    @Column(name = "transaction_id", unique = true)
    private String transactionId; // Thêm thuộc tính này

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
